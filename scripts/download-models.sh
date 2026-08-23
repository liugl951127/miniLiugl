#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# 下载 Phase 1 本地多模态 ONNX 模型
# ─────────────────────────────────────────────────────────────
# 3 个模型:
#   1. CLIP ViT-B/32        (text+image 512-dim, 350MB) - 以文搜图
#   2. ResNet50             (image classification, 100MB) - 1000 类
#   3. YOLOv8n              (object detection, 13MB) - 实时目标检测
# ─────────────────────────────────────────────────────────────
# Source: HuggingFace 镜像 (https://hf-mirror.com)
#         或官方 (https://huggingface.co)
#
# Usage:
#   ./scripts/download-models.sh         # 下载全部
#   ./scripts/download-models.sh clip    # 只下 CLIP
#   ./scripts/download-models.sh resnet  # 只下 ResNet50
#   ./scripts/download-models.sh yolo    # 只下 YOLOv8n
# ─────────────────────────────────────────────────────────────

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODELS_DIR="$ROOT/data/models"
mkdir -p "$MODELS_DIR"

# 镜像: 国内优先 hf-mirror.com, 失败回退 huggingface.co
HF_BASE="${HF_BASE:-https://hf-mirror.com}"

log() { echo "▸ $*"; }
ok()  { echo "✅ $*"; }
warn(){ echo "⚠️  $*"; }
err() { echo "❌ $*" >&2; }

download() {
    local url="$1"
    local out="$2"
    log "下载 $out"
    log "  from: $url"
    if curl -fL --retry 3 --connect-timeout 15 -o "$out" "$url"; then
        local size=$(du -h "$out" | cut -f1)
        ok "完成 $out ($size)"
    else
        err "下载失败: $url"
        rm -f "$out"
        return 1
    fi
}

# ─── 1. CLIP ViT-B/32 ────────────────────────────────────
download_clip() {
    log "=== CLIP ViT-B/32 (OpenAI, 512-dim) ==="
    cd "$MODELS_DIR"

    # 优先 onnx-community 的 ONNX 版
    # 也可自己转: optimum-cli export onnx --model openai/clip-vit-base-patch32 clip-vit-base-patch32
    local clip_dir="$MODELS_DIR/clip-vit-base-patch32"
    mkdir -p "$clip_dir"

    download "$HF_BASE/onnx-community/clip-vit-base-patch32/resolve/main/onnx/model.onnx" \
             "$clip_dir/model.onnx" || true

    # tokenizer/text preprocessor
    download "$HF_BASE/onnx-community/clip-vit-base-patch32/resolve/main/tokenizer.json" \
             "$clip_dir/tokenizer.json" || true
    download "$HF_BASE/onnx-community/clip-vit-base-patch32/resolve/main/preprocessor_config.json" \
             "$clip_dir/preprocessor_config.json" || true
    download "$HF_BASE/openai/clip-vit-base-patch32/resolve/main/vocab.json" \
             "$clip_dir/vocab.json" || true
    download "$HF_BASE/openai/clip-vit-base-patch32/resolve/main/merges.txt" \
             "$clip_dir/merges.txt" || true
}

# ─── 2. ResNet50 ─────────────────────────────────────────
download_resnet() {
    log "=== ResNet50 (ONNX, 1000 classes ImageNet) ==="
    local resnet_dir="$MODELS_DIR/resnet50"
    mkdir -p "$resnet_dir"

    # onnx-model-zoo 版
    download "$HF_BASE/onnx-community/resnet50/resolve/main/onnx/model.onnx" \
             "$resnet_dir/model.onnx" || \
    download "https://github.com/onnx/models/raw/main/validated/vision/classification/resnet/model/resnet50-v2-7.onnx" \
             "$resnet_dir/model.onnx" || true
}

# ─── 3. YOLOv8n ──────────────────────────────────────────
download_yolo() {
    log "=== YOLOv8n (Ultralytics, 80 classes COCO) ==="
    local yolo_dir="$MODELS_DIR/yolov8n"
    mkdir -p "$yolo_dir"

    # yolov8n.onnx from ultralytics assets
    download "$HF_BASE/Ultralytics/YOLOv8/resolve/main/yolov8n.onnx" \
             "$yolo_dir/model.onnx" || \
    download "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.onnx" \
             "$yolo_dir/model.onnx" || true

    # COCO 类别
    download "$HF_BASE/ultralytics/yolov5/resolve/main/data/coco128.yaml" \
             "$yolo_dir/coco.yaml" || true
}

# ─── 主流程 ──────────────────────────────────────────────
case "${1:-all}" in
    clip)     download_clip ;;
    resnet)   download_resnet ;;
    yolo)     download_yolo ;;
    whisper)  download_whisper ;;
    vad)      download_vad ;;
    audio)    download_whisper; download_vad ;;
    all|"")
        download_clip
        download_resnet
        download_yolo
        download_whisper
        download_vad
        ;;
    *)
        err "Unknown arg: $1"
        exit 1
        ;;
esac

echo ""
echo "═══════════════════════════════════════════════════"
echo "模型下载完成 → $MODELS_DIR"
ls -lhR "$MODELS_DIR" | head -30
echo ""
echo "环境变量: export MINIMAX_AI_MODELS=$MODELS_DIR"
echo "═══════════════════════════════════════════════════"
