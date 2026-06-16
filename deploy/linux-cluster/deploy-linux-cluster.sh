#!/bin/bash
# ============================================================
#  MiniMax Platform - Linux 集群部署脚本
#  适用: Kubernetes 1.24+  或  Docker Swarm / docker-compose 集群
#  模式: k8s (推荐) | swarm | compose
#  作者: Mavis  日期: 2026-06-16
# ============================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K8S_DIR="$SCRIPT_DIR/k8s"
COMPOSE_CLUSTER_DIR="$SCRIPT_DIR/compose-cluster"

MODE="${1:-k8s}"  # k8s | swarm | compose
NAMESPACE="${2:-minimax}"

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}  MiniMax Platform 集群部署${NC}"
echo -e "${BLUE}  模式: $MODE    命名空间: $NAMESPACE${NC}"
echo -e "${BLUE}============================================================${NC}"
echo

# ============================================================
# 模式 1: Kubernetes 部署 (推荐)
# ============================================================
if [ "$MODE" = "k8s" ]; then
    echo -e "${YELLOW}[0/10] 检查 kubectl + 集群连通...${NC}"
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}✗ kubectl 未安装${NC}"
        exit 1
    fi
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}✗ 无法连接 Kubernetes 集群${NC}"
        echo "  检查 ~/.kube/config 或运行: kubectl cluster-info"
        exit 1
    fi
    echo -e "${GREEN}✓ 集群可达${NC}"
    echo

    # 生成 K8s manifests (如果还没生成)
    if [ ! -d "$K8S_DIR" ]; then
        echo -e "${RED}✗ K8s manifest 目录不存在: $K8S_DIR${NC}"
        exit 1
    fi

    # ---------- 1) Namespace ----------
    echo -e "${YELLOW}[1/10] 创建命名空间: $NAMESPACE${NC}"
    kubectl apply -f "$K8S_DIR/00-namespace.yaml" 2>/dev/null || \
        kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    echo

    # ---------- 2) Secrets + ConfigMap ----------
    echo -e "${YELLOW}[2/10] 应用 Secrets + ConfigMap...${NC}"
    # 生成密码 (生产应从 secret manager 注入)
    MYSQL_PASS="${MYSQL_PASS:-MinMax2026!}"
    REDIS_PASS="${REDIS_PASS:-MinMax2026!}"
    JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 | tr -d '\n')}"

    cat <<EOF | kubectl apply -n $NAMESPACE -f -
apiVersion: v1
kind: Secret
metadata:
  name: minimax-secrets
type: Opaque
stringData:
  mysql-root-password: "$MYSQL_PASS"
  mysql-password: "$MYSQL_PASS"
  redis-password: "$REDIS_PASS"
  jwt-secret: "$JWT_SECRET"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: minimax-config
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: minimax-platform
      datasource:
        url: jdbc:mysql://mysql-svc:3306/minimax?useUnicode=true&characterEncoding=utf8&useSSL=false
        username: minimax
        password: $MYSQL_PASS
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
      data:
        redis:
          host: redis-svc
          port: 6379
          password: $REDIS_PASS
    minimax:
      jwt:
        secret: $JWT_SECRET
EOF
    echo

    # ---------- 3) MySQL (StatefulSet) ----------
    echo -e "${YELLOW}[3/10] 部署 MySQL (StatefulSet)...${NC}"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/10-mysql-statefulset.yaml"
    echo "等待 MySQL 就绪..."
    kubectl wait --for=condition=ready pod -l app=mysql -n $NAMESPACE --timeout=180s
    echo -e "${GREEN}✓ MySQL 就绪${NC}"
    echo

    # ---------- 4) 初始化 DB ----------
    echo -e "${YELLOW}[4/10] 初始化数据库 (执行 SQL)...${NC}"
    for sql_file in "$PROJECT_ROOT/sql/init/"*.sql; do
        if [ -f "$sql_file" ]; then
            echo "  $(basename $sql_file) ..."
            # 用 kubectl cp + exec 注入
            kubectl cp "$sql_file" "$NAMESPACE/$(kubectl get pod -n $NAMESPACE -l app=mysql -o jsonpath='{.items[0].metadata.name}'):/tmp/$(basename $sql_file)"
            kubectl exec -n $NAMESPACE deploy/mysql -- bash -c \
                "mysql -uroot -p$MYSQL_PASS minimax < /tmp/$(basename $sql_file)" \
                && echo -e "    ${GREEN}✓${NC}" \
                || echo -e "    ${YELLOW}! 继续${NC}"
        fi
    done
    echo

    # ---------- 5) Redis ----------
    echo -e "${YELLOW}[5/10] 部署 Redis (Deployment + Service)...${NC}"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/20-redis.yaml"
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=60s
    echo -e "${GREEN}✓ Redis 就绪${NC}"
    echo

    # ---------- 6) 推送/导入应用镜像 ----------
    echo -e "${YELLOW}[6/10] 镜像处理...${NC}"
    REGISTRY="${REGISTRY:-localhost:5000}"  # 默认用本地 registry
    TAG="${TAG:-v1.0.0}"

    if [ "$REGISTRY" != "skip" ]; then
        echo "  Build & Tag 镜像到 $REGISTRY ..."
        for svc in auth chat memory model rag gateway; do
            docker build \
                -t "$REGISTRY/minimax-$svc:$TAG" \
                -f "$PROJECT_ROOT/deploy/docker/Dockerfile.$svc" \
                "$PROJECT_ROOT" 2>/dev/null \
                && echo "    ✓ minimax-$svc" \
                || echo "    ! $svc 镜像构建失败 (可能无 Dockerfile, 用现有镜像)"
        done

        echo "  Push 镜像..."
        for svc in auth chat memory model rag gateway; do
            docker push "$REGISTRY/minimax-$svc:$TAG" 2>/dev/null || true
        done
    else
        echo "  REGISTRY=skip, 假设镜像已在集群内"
    fi
    echo

    # ---------- 7) 应用 Deployment ----------
    echo -e "${YELLOW}[7/10] 部署应用服务 (auth/chat/memory/model/rag/gateway)...${NC}"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/30-auth.yaml"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/31-chat.yaml"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/32-memory.yaml"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/33-model.yaml"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/34-rag.yaml"
    kubectl apply -n $NAMESPACE -f "$K8S_DIR/35-gateway.yaml"
    echo "等待应用就绪..."
    kubectl wait --for=condition=available deployment -l app.kubernetes.io/part-of=minimax -n $NAMESPACE --timeout=300s
    echo

    # ---------- 8) Ingress ----------
    echo -e "${YELLOW}[8/10] 配置 Ingress (对外暴露)...${NC}"
    if [ -f "$K8S_DIR/40-ingress.yaml" ]; then
        kubectl apply -n $NAMESPACE -f "$K8S_DIR/40-ingress.yaml"
    else
        echo "  使用 port-forward 访问: kubectl port-forward -n $NAMESPACE svc/gateway 8080:8080"
    fi
    echo

    # ---------- 9) HPA (自动扩缩) ----------
    echo -e "${YELLOW}[9/10] 配置 HPA (自动扩缩)...${NC}"
    if [ -f "$K8S_DIR/50-hpa.yaml" ]; then
        kubectl apply -n $NAMESPACE -f "$K8S_DIR/50-hpa.yaml"
    fi
    echo

    # ---------- 10) 验证 ----------
    echo -e "${YELLOW}[10/10] 验证部署...${NC}"
    echo
    echo "Pods 状态:"
    kubectl get pods -n $NAMESPACE -o wide
    echo
    echo "Services:"
    kubectl get svc -n $NAMESPACE
    echo
    echo "端口转发 (开发用):"
    echo "  kubectl port-forward -n $NAMESPACE svc/gateway 8080:8080"
    echo
    echo -e "${GREEN}============================================================${NC}"
    echo -e "${GREEN}  K8s 部署完成！${NC}"
    echo -e "${GREEN}============================================================${NC}"
    echo
    echo "默认账号: admin / admin@123"
    echo
    echo "运维命令:"
    echo "  查看日志:   kubectl logs -n $NAMESPACE -l app=auth -f"
    echo "  扩缩:       kubectl scale -n $NAMESPACE deploy/auth --replicas=3"
    echo "  删除:       kubectl delete namespace $NAMESPACE"
    echo
    exit 0
fi

# ============================================================
# 模式 2: Docker Swarm 集群
# ============================================================
if [ "$MODE" = "swarm" ]; then
    echo -e "${YELLOW}[0/8] 检查 Docker Swarm...${NC}"
    if ! docker info 2>/dev/null | grep -q "Swarm: active"; then
        echo -e "${RED}✗ 当前节点不在 Swarm 集群${NC}"
        echo "  初始化: docker swarm init"
        exit 1
    fi
    echo -e "${GREEN}✓ Swarm 已激活${NC}"
    echo

    cd "$PROJECT_ROOT"

    # 1) 初始化网络
    echo -e "${YELLOW}[1/8] 创建 overlay 网络...${NC}"
    docker network create --driver overlay minimax-net 2>/dev/null || true
    echo

    # 2) Secrets
    echo -e "${YELLOW}[2/8] 创建 Swarm Secrets...${NC}"
    echo "MinMax2026!" | docker secret create minimax_mysql_password - 2>/dev/null || true
    echo "MinMax2026!" | docker secret create minimax_redis_password - 2>/dev/null || true
    echo

    # 3) Stack 部署
    echo -e "${YELLOW}[3/8] 部署 Swarm Stack: minimax${NC}"
    if [ ! -f "$COMPOSE_CLUSTER_DIR/docker-stack.yml" ]; then
        echo -e "${RED}✗ 缺少 $COMPOSE_CLUSTER_DIR/docker-stack.yml${NC}"
        exit 1
    fi
    docker stack deploy -c "$COMPOSE_CLUSTER_DIR/docker-stack.yml" minimax
    echo

    # 4) 等待就绪
    echo -e "${YELLOW}[4/8] 等待服务启动...${NC}"
    sleep 30
    docker stack ps minimax
    echo

    # 5) 扩缩
    echo -e "${YELLOW}[5/8] 扩缩到 3 副本...${NC}"
    docker service scale minimax_auth=3 minimax_chat=3 minimax_model=3 minimax_memory=3 minimax_rag=3
    echo

    # 6) 初始化 DB
    echo -e "${YELLOW}[6/8] 初始化数据库...${NC}"
    for sql_file in "$PROJECT_ROOT/sql/init/"*.sql; do
        [ -f "$sql_file" ] && \
            docker exec -i $(docker ps -q -f name=minimax_mysql) \
            mysql -uroot -pMinMax2026! minimax < "$sql_file" 2>/dev/null || true
    done
    echo

    echo -e "${YELLOW}[7/8] 验证服务...${NC}"
    echo "Services:"
    docker service ls | grep minimax
    echo

    echo -e "${YELLOW}[8/8] 完成${NC}"
    echo -e "${GREEN}✓ Swarm 部署完成${NC}"
    echo
    echo "查看服务:  docker service ls"
    echo "扩缩:      docker service scale minimax_auth=5"
    echo "删除:      docker stack rm minimax"
    echo
    exit 0
fi

# ============================================================
# 模式 3: docker-compose 模拟集群 (单机多副本)
# ============================================================
if [ "$MODE" = "compose" ]; then
    echo -e "${YELLOW}[0/8] docker-compose 集群模式 (单机多副本)${NC}"
    cd "$PROJECT_ROOT"

    if [ ! -f "$COMPOSE_CLUSTER_DIR/docker-compose.cluster.yml" ]; then
        echo -e "${RED}✗ 缺少 $COMPOSE_CLUSTER_DIR/docker-compose.cluster.yml${NC}"
        exit 1
    fi

    # 1) 启动
    echo -e "${YELLOW}[1/8] 启动集群...${NC}"
    docker compose -f "$COMPOSE_CLUSTER_DIR/docker-compose.cluster.yml" up -d
    echo

    # 2) 等待
    echo -e "${YELLOW}[2/8] 等待启动...${NC}"
    sleep 30
    echo

    # 3) 初始化 DB
    echo -e "${YELLOW}[3/8] 初始化数据库...${NC}"
    for sql_file in "$PROJECT_ROOT/sql/init/"*.sql; do
        [ -f "$sql_file" ] && \
            docker exec -i $(docker ps -q -f name=minimax-mysql) \
            mysql -uroot -pMinMax2026! minimax < "$sql_file" 2>/dev/null || true
    done
    echo

    # 4) 验证
    echo -e "${YELLOW}[4/8] 验证...${NC}"
    docker compose -f "$COMPOSE_CLUSTER_DIR/docker-compose.cluster.yml" ps
    echo
    echo -e "${GREEN}✓ 集群部署完成 (单机多副本)${NC}"
    echo "停止: docker compose -f $COMPOSE_CLUSTER_DIR/docker-compose.cluster.yml down"
    exit 0
fi

echo -e "${RED}未知模式: $MODE${NC}"
echo "用法: $0 [k8s|swarm|compose] [namespace]"
exit 1
