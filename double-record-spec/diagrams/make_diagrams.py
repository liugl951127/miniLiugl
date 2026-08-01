"""
Generate architecture diagrams and flow charts for the dual-record specification document.
Output: PNG files in /workspace/double-record-spec/diagrams/
Theme: dark blue (#2b2d42) + red (#d90429) + light gray (#edf2f4)
"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, Rectangle, FancyArrowPatch, Circle
from matplotlib.lines import Line2D
import os

OUT = os.path.dirname(os.path.abspath(__file__))

# Color palette (consistent with PPT)
PRIMARY   = "#2b2d42"   # deep blue - main
SECONDARY = "#8d99ae"   # medium gray
ACCENT    = "#d90429"   # red - highlight
LIGHT     = "#edf2f4"   # light gray - card bg
DARKER    = "#1a1c2e"   # darker blue
SUCCESS   = "#2A9D8F"   # green
WARNING   = "#E9C46A"   # yellow
DANGER    = "#E76F51"   # orange
WHITE     = "#FFFFFF"

# Chinese font setup
matplotlib.rcParams['font.sans-serif'] = ['Noto Sans CJK SC', 'WenQuanYi Zen Hei', 'Microsoft YaHei', 'SimHei', 'DejaVu Sans']
matplotlib.rcParams['axes.unicode_minus'] = False

def setup_fig(w=14, h=8, dpi=150, y_max=100):
    fig, ax = plt.subplots(figsize=(w, h), dpi=dpi)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, y_max)
    ax.set_aspect('equal')
    ax.axis('off')
    fig.patch.set_facecolor(WHITE)
    return fig, ax

def rounded_box(ax, x, y, w, h, text, fill=LIGHT, edge=PRIMARY, text_color=PRIMARY, fontsize=10, bold=False, radius=1.5):
    box = FancyBboxPatch((x, y), w, h,
                         boxstyle=f"round,pad=0,rounding_size={radius}",
                         linewidth=1.2, edgecolor=edge, facecolor=fill)
    ax.add_patch(box)
    weight = "bold" if bold else "normal"
    ax.text(x + w/2, y + h/2, text, ha="center", va="center",
            color=text_color, fontsize=fontsize, fontweight=weight, wrap=True)

def rectangle(ax, x, y, w, h, text, fill=PRIMARY, text_color=WHITE, fontsize=10, bold=False):
    box = Rectangle((x, y), w, h, linewidth=0, facecolor=fill)
    ax.add_patch(box)
    weight = "bold" if bold else "normal"
    ax.text(x + w/2, y + h/2, text, ha="center", va="center",
            color=text_color, fontsize=fontsize, fontweight=weight, wrap=True)

def arrow(ax, x1, y1, x2, y2, color=PRIMARY, width=0.4, style="-|>"):
    arr = FancyArrowPatch((x1, y1), (x2, y2),
                          arrowstyle=style, mutation_scale=15,
                          color=color, linewidth=width)
    ax.add_patch(arr)

def label(ax, x, y, text, color=SECONDARY, fontsize=8, italic=False):
    weight = "normal"
    style = "italic" if italic else "normal"
    ax.text(x, y, text, ha="center", va="center", color=color,
            fontsize=fontsize, fontweight=weight, fontstyle=style)

# ─────────────────────────────────────────────────────────
# 1. 整体技术架构图 (5 layers + cross-cutting)
# ─────────────────────────────────────────────────────────
def diagram_01_overall_architecture():
    fig, ax = setup_fig(16, 12, y_max=110)
    # Title
    ax.text(50, 105, "图3-1  整体技术架构图 (5层 + 横切能力)", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 101, "Source: 双录一体化平台架构设计 V1.0", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # 5 layers on left
    layers = [
        ("接入层",      ["线上 H5/小程序", "线下一体机", "PAD 移动展业", "网点 PC 端"]),
        ("流程编排层",  ["BPMN 流程引擎", "状态机", "异常分支", "回退补偿"]),
        ("能力中台层",  ["话术中心", "双录引擎", "智能质检", "电子签", "风评引擎"]),
        ("数据治理层",  ["统一订单中心", "分布式事务", "客户主数据", "影像存证", "审计追溯"]),
        ("基础底座层",  ["国密加密", "对象存储", "消息中间件", "统一身份", "监管上报"]),
    ]
    layer_h = 13
    layer_y_start = 84
    for i, (name, modules) in enumerate(layers):
        y = layer_y_start - i * (layer_h + 1.5)
        # Layer name block
        rectangle(ax, 4, y, 16, layer_h, name, fill=PRIMARY, text_color=WHITE, fontsize=12, bold=True)
        # Modules block
        rect = FancyBboxPatch((21, y), 38, layer_h,
                              boxstyle="round,pad=0,rounding_size=0.8",
                              linewidth=1, edgecolor=SECONDARY, facecolor=LIGHT)
        ax.add_patch(rect)
        # Module text
        mod_text = "  |  ".join(modules)
        ax.text(40, y + layer_h/2, mod_text, ha="center", va="center",
                color=PRIMARY, fontsize=9.5)

    # Cross-cutting capabilities on right
    cross_x = 64
    cross_w = 32
    rect = FancyBboxPatch((cross_x, 8), cross_w, 80,
                          boxstyle="round,pad=0,rounding_size=1",
                          linewidth=1.2, edgecolor=ACCENT, facecolor=WHITE)
    ax.add_patch(rect)
    rectangle(ax, cross_x, 82, cross_w, 6, "横切能力 (贯穿所有层)", fill=ACCENT, text_color=WHITE, fontsize=12, bold=True)

    cross = [
        ("统一话术中心", "话术/风险点/产品参数/合规要点统一管理"),
        ("智能质检引擎", "规则 + ASR/NLP/情绪 多模态 AI 质检"),
        ("全链路追踪",   "预约→双录→签单→归档 全链路埋点"),
        ("合规审计",     "监管接口直连 · 全量留痕 · 可回放"),
        ("可视化运营",   "管理者驾驶舱 · 实时大屏 · 异常告警"),
    ]
    cy = 74
    for t, d in cross:
        # dot
        c = Circle((cross_x + 2.5, cy + 0.5), 0.8, color=ACCENT)
        ax.add_patch(c)
        ax.text(cross_x + 5, cy + 2, t, ha="left", va="center",
                color=PRIMARY, fontsize=10, fontweight="bold")
        ax.text(cross_x + 5, cy - 1.5, d, ha="left", va="center",
                color=SECONDARY, fontsize=8)
        cy -= 13

    # Layer connection lines
    for i in range(4):
        y1 = layer_y_start - i * (layer_h + 1.5)
        y2 = layer_y_start - (i + 1) * (layer_h + 1.5)
        arrow(ax, 12, y1, 12, y2 + layer_h, color=SECONDARY, width=0.3)

    # Cross-cutting lines (5 horizontal arrows from cross to each layer)
    for i in range(5):
        y = layer_y_start - i * (layer_h + 1.5) + layer_h / 2
        arrow(ax, 60, y, cross_x, y, color=ACCENT, width=0.25, style="<|-|>")

    plt.savefig(os.path.join(OUT, "01_overall_architecture.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 2. 端到端业务流程图
# ─────────────────────────────────────────────────────────
def diagram_02_end_to_end_flow():
    fig, ax = setup_fig(16, 11)
    ax.text(50, 96, "图8-1  端到端业务流程 (预约 → 归档)", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 93, "Source: 主流程与关键 Gate", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    stages = [
        ("01\n预约", "客户身份预校验\n+ 产品匹配",  "G1 实名核身通过"),
        ("02\n排程", "客户经理排期\n+ 渠道分配",  ""),
        ("03\n核身", "证件 OCR + 活体\n+ 双签",     "G2 风评等级\n与产品匹配"),
        ("04\n风评", "KYC 问卷\n+ 风险等级评定",   ""),
        ("05\n双录", "话术执行\n+ 音视频同步录制", "G3 话术 100% 执行\nG4 客户意愿\n明确确认"),
        ("06\n签约", "电子合同\n+ CA 数字签名",   ""),
        ("07\n质检", "AI 智能\n+ 人工复核",       "G5 质检分数 >= 70"),
        ("08\n归档", "区块链存证\n+ 监管上报",     ""),
    ]

    sw = 11
    sh = 24
    gap = 1
    sx = 4
    sy = 50

    for i, (n, desc, gate) in enumerate(stages):
        x = sx + i * (sw + gap)
        # Stage card
        rect = FancyBboxPatch((x, sy), sw, sh,
                              boxstyle="round,pad=0,rounding_size=1",
                              linewidth=1.2, edgecolor=PRIMARY, facecolor=LIGHT)
        ax.add_patch(rect)
        # Top color bar
        rectangle(ax, x, sy + sh - 5, sw, 5, n, fill=PRIMARY, text_color=ACCENT, fontsize=10, bold=True)
        # Description
        ax.text(x + sw/2, sy + sh/2 - 1, desc, ha="center", va="center",
                color=PRIMARY, fontsize=8.5)
        # Gate indicator
        if gate:
            rect2 = FancyBboxPatch((x, sy - 8), sw, 7,
                                   boxstyle="round,pad=0,rounding_size=0.5",
                                   linewidth=0.8, edgecolor=ACCENT, facecolor=ACCENT)
            ax.add_patch(rect2)
            ax.text(x + sw/2, sy - 4.5, gate, ha="center", va="center",
                    color=WHITE, fontsize=7.5, fontweight="bold")

        # Arrow
        if i < len(stages) - 1:
            arrow(ax, x + sw + 0.2, sy + sh/2, x + sw + gap - 0.2, sy + sh/2,
                  color=ACCENT, width=0.5)

    # Channel branches below
    branch_y = 18
    rect = FancyBboxPatch((2, branch_y - 3), 96, 16,
                          boxstyle="round,pad=0,rounding_size=1",
                          linewidth=1, edgecolor=SECONDARY, facecolor=WHITE)
    ax.add_patch(rect)
    ax.text(50, branch_y + 10, "渠道分支 (同一流程 · 不同载体)", ha="center", va="center",
            color=PRIMARY, fontsize=11, fontweight="bold")
    branches = [
        ("线上 H5/小程序",  "全程 5-8 分钟\n客户自助"),
        ("线下一体机(高柜)","客户经理陪同\n15-20 分钟"),
        ("PAD 移动展业",   "外拓场景\n10-15 分钟"),
    ]
    bw = 28
    for i, (n, d) in enumerate(branches):
        x = 5 + i * 32
        rect = FancyBboxPatch((x, branch_y - 2), bw, 9,
                              boxstyle="round,pad=0,rounding_size=0.6",
                              linewidth=0.8, edgecolor=ACCENT, facecolor=LIGHT)
        ax.add_patch(rect)
        ax.text(x + bw/2, branch_y + 4, n, ha="center", va="center",
                color=PRIMARY, fontsize=10, fontweight="bold")
        ax.text(x + bw/2, branch_y, d, ha="center", va="center",
                color=SECONDARY, fontsize=8)

    plt.savefig(os.path.join(OUT, "02_end_to_end_flow.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 3. 状态机图
# ─────────────────────────────────────────────────────────
def diagram_03_state_machine():
    fig, ax = setup_fig(15, 9)
    ax.text(50, 95, "图7-1  双录订单状态机", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 91, "Source: S0→S6 全状态流转,任一失败可回退到上一状态", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    states = [
        ("S0", "已预约",        SECONDARY),
        ("S1", "客户已核验",    SECONDARY),
        ("S2", "话术执行中",    SECONDARY),
        ("S3", "视频录制中",    PRIMARY),
        ("S4", "电子签约",      PRIMARY),
        ("S5", "质检通过",      SUCCESS),
        ("S6", "订单完成",      ACCENT),
    ]

    sw = 11
    sh = 18
    gap = 2
    sx = 5
    sy = 55

    for i, (n, name, color) in enumerate(states):
        x = sx + i * (sw + gap)
        rect = FancyBboxPatch((x, sy), sw, sh,
                              boxstyle="round,pad=0,rounding_size=0.8",
                              linewidth=1.5, edgecolor=color, facecolor=color)
        ax.add_patch(rect)
        ax.text(x + sw/2, sy + sh/2 + 3, n, ha="center", va="center",
                color=WHITE, fontsize=14, fontweight="bold")
        ax.text(x + sw/2, sy + sh/2 - 3, name, ha="center", va="center",
                color=WHITE, fontsize=10)
        if i < len(states) - 1:
            arrow(ax, x + sw + 0.1, sy + sh/2, x + sw + gap - 0.1, sy + sh/2,
                  color=ACCENT, width=0.6)

    # Trigger events on arrows
    events = ["排期确认", "证件通过", "节点完成", "录制完成", "签署完成", "质检通过"]
    for i, e in enumerate(events):
        x_mid = sx + i * (sw + gap) + sw + gap/2
        ax.text(x_mid, sy + sh/2 + 3, e, ha="center", va="bottom",
                color=SECONDARY, fontsize=7.5, fontstyle="italic")

    # Compensation path
    comp_y = 25
    ax.text(50, comp_y + 8, "Saga 补偿路径 (任一失败)", ha="center", va="center",
            color=ACCENT, fontsize=11, fontweight="bold")
    rect = FancyBboxPatch((15, comp_y - 5), 70, 10,
                          boxstyle="round,pad=0,rounding_size=0.6",
                          linewidth=1, edgecolor=ACCENT, facecolor=WHITE)
    ax.add_patch(rect)
    comp_text = "状态回退 | 资源释放 | 通知客户经理 | 异常日志记录 | 30s 内自动重试 (最多 3 次)"
    ax.text(50, comp_y, comp_text, ha="center", va="center",
            color=PRIMARY, fontsize=10)

    # Legend
    leg_y = 8
    legend_items = [
        (SECONDARY, "未开始"),
        (PRIMARY,   "进行中"),
        (SUCCESS,   "已通过"),
        (ACCENT,    "已完成"),
    ]
    for i, (c, t) in enumerate(legend_items):
        x = 15 + i * 18
        c_box = Rectangle((x, leg_y), 2.5, 2.5, facecolor=c, edgecolor="none")
        ax.add_patch(c_box)
        ax.text(x + 4, leg_y + 1.25, t, ha="left", va="center",
                color=PRIMARY, fontsize=8.5)

    plt.savefig(os.path.join(OUT, "03_state_machine.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 4. Saga 分布式事务图
# ─────────────────────────────────────────────────────────
def diagram_04_saga():
    fig, ax = setup_fig(15, 10)
    ax.text(50, 95, "图7-2  Saga 分布式事务时序", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 91, "Source: T1-T6 事务链 + 补偿机制", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # 6 stages vertically
    stages = [
        ("T1 预占资源",  "创建订单主记录\n视频 OSS 路径预分配",     "10ms"),
        ("T2 双录录制",  "音视频采集 → SM4 加密 → SHA256 校验",    "5-15min"),
        ("T3 风险评估",  "调用风评引擎 → 落库 → 客户风险画像",     "200ms"),
        ("T4 电子签约",  "客户人脸核身 + 意愿确认 + CA 数字签名",  "30s"),
        ("T5 智能质检",  "异步发起 L1/L2/L3 多层质检",              "异步"),
        ("T6 订单回写",  "CRM/核心系统同步 + 区块链存证",          "1s"),
    ]
    sh = 11
    sy = 80
    for i, (n, d, time) in enumerate(stages):
        y = sy - i * (sh + 1.5)
        # Stage label box (left)
        rect = FancyBboxPatch((4, y), 18, sh,
                              boxstyle="round,pad=0,rounding_size=0.6",
                              linewidth=1, edgecolor=PRIMARY, facecolor=PRIMARY)
        ax.add_patch(rect)
        ax.text(13, y + sh/2 + 1.5, n, ha="center", va="center",
                color=WHITE, fontsize=11, fontweight="bold")
        ax.text(13, y + sh/2 - 1.5, time, ha="center", va="center",
                color=ACCENT, fontsize=8)
        # Description
        rect = FancyBboxPatch((24, y), 56, sh,
                              boxstyle="round,pad=0,rounding_size=0.5",
                              linewidth=0.8, edgecolor=SECONDARY, facecolor=LIGHT)
        ax.add_patch(rect)
        ax.text(52, y + sh/2, d, ha="center", va="center",
                color=PRIMARY, fontsize=10)
        # Right: success/fail
        ax.text(86, y + sh/2, "OK 成功 → 下一阶段", ha="left", va="center",
                color=SUCCESS, fontsize=8.5, fontweight="bold")

    # Vertical arrow connecting stages
    arrow(ax, 13, 80, 13, 80 - 6 * (sh + 1.5) + sh, color=ACCENT, width=0.4)

    # Compensation path on far right
    comp_x = 86
    arrow(ax, 13, 80 - 6 * (sh + 1.5) + sh/2, comp_x - 1, 80 - 6 * (sh + 1.5) + sh/2,
          color=ACCENT, width=0.3)
    ax.text(comp_x + 1, 80 - 6 * (sh + 1.5) + sh/2, "若失败 → 触发补偿", ha="left", va="center",
            color=ACCENT, fontsize=8, fontweight="bold")

    # Compensation actions
    comp_y = 8
    rect = FancyBboxPatch((4, comp_y - 2), 92, 8,
                          boxstyle="round,pad=0,rounding_size=0.6",
                          linewidth=1, edgecolor=ACCENT, facecolor=WHITE)
    ax.add_patch(rect)
    ax.text(50, comp_y + 4, "补偿动作 (Compensation)", ha="center", va="center",
            color=ACCENT, fontsize=10, fontweight="bold")
    comp_actions = "释放 OSS 路径  |  删除临时订单  |  回滚风评数据  |  吊销签名证书  |  通知客户经理"
    ax.text(50, comp_y, comp_actions, ha="center", va="center",
            color=PRIMARY, fontsize=8.5)

    plt.savefig(os.path.join(OUT, "04_saga_transaction.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 5. 话术原子化结构图
# ─────────────────────────────────────────────────────────
def diagram_05_script_atomic():
    fig, ax = setup_fig(15, 10)
    ax.text(50, 95, "图9-1  话术原子化结构", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 91, "Source: 节点独立 + 强制确认 + 不可跳过", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # Master script
    master_x, master_y, master_w, master_h = 8, 76, 30, 12
    rect = FancyBboxPatch((master_x, master_y), master_w, master_h,
                          boxstyle="round,pad=0,rounding_size=1",
                          linewidth=1.5, edgecolor=PRIMARY, facecolor=PRIMARY)
    ax.add_patch(rect)
    ax.text(master_x + master_w/2, master_y + master_h/2, "话术模板 (主)", ha="center", va="center",
            color=WHITE, fontsize=12, fontweight="bold")

    # 6 atomic nodes
    nodes = [
        ("N1 问候核身",  "客户身份确认",     "强制读"),
        ("N2 产品告知",  "产品要素告知",     "强制读"),
        ("N3 风险揭示",  "关键风险点",       "强制读+确认"),
        ("N4 适当性匹配","风险等级匹配",     "强制读+确认"),
        ("N5 犹豫期/条款","退保权利告知",    "强制读+确认"),
        ("N6 客户意愿",  "最终确认投保",     "强制读+签字"),
    ]
    nw = 13
    nh = 12
    ny = 50
    nx_start = 3
    gap = 2
    for i, (n, d, ctl) in enumerate(nodes):
        x = nx_start + i * (nw + gap)
        rect = FancyBboxPatch((x, ny), nw, nh,
                              boxstyle="round,pad=0,rounding_size=0.6",
                              linewidth=1.2, edgecolor=PRIMARY, facecolor=LIGHT)
        ax.add_patch(rect)
        # Header
        rect = Rectangle((x, ny + nh - 3), nw, 3, facecolor=PRIMARY, edgecolor="none")
        ax.add_patch(rect)
        ax.text(x + nw/2, ny + nh - 1.5, n, ha="center", va="center",
                color=WHITE, fontsize=9, fontweight="bold")
        # Description
        ax.text(x + nw/2, ny + nh/2 - 1, d, ha="center", va="center",
                color=PRIMARY, fontsize=8)
        # Control type
        ctl_color = ACCENT if "强制" in ctl else SECONDARY
        ax.text(x + nw/2, ny + 1.5, ctl, ha="center", va="center",
                color=ctl_color, fontsize=7.5, fontweight="bold")
        # Connecting line from master
        arrow(ax, master_x + master_w/2, master_y, x + nw/2, ny + nh,
              color=SECONDARY, width=0.3)
        # Arrows between nodes
        if i < len(nodes) - 1:
            arrow(ax, x + nw + 0.1, ny + nh/2, x + nw + gap - 0.1, ny + nh/2,
                  color=ACCENT, width=0.4)

    # Skip protection
    skip_y = 28
    rect = FancyBboxPatch((8, skip_y), 84, 8,
                          boxstyle="round,pad=0,rounding_size=0.6",
                          linewidth=1, edgecolor=ACCENT, facecolor=WHITE)
    ax.add_patch(rect)
    ax.text(50, skip_y + 4, "强制约束 (任一条件不满足 → 流程不可继续)", ha="center", va="center",
            color=ACCENT, fontsize=10, fontweight="bold")
    constraints = "客户端必须完整听读  |  客服端必须完整陈述  |  客户必须明确回答「是」  |  强制确认位必须由客户本人口头表达"
    ax.text(50, skip_y + 1, constraints, ha="center", va="center",
            color=PRIMARY, fontsize=8.5)

    # Storage & sync
    storage_y = 8
    rect = FancyBboxPatch((8, storage_y), 84, 12,
                          boxstyle="round,pad=0,rounding_size=0.6",
                          linewidth=1, edgecolor=PRIMARY, facecolor=LIGHT)
    ax.add_patch(rect)
    ax.text(50, storage_y + 9, "存储与同步", ha="center", va="center",
            color=PRIMARY, fontsize=10, fontweight="bold")
    storage_text = "中心化话术库 (MySQL)  →  Redis 缓存  →  线上 SDK 拉取  +  线下 PAD/一体机 MD5 校验拉取"
    ax.text(50, storage_y + 3, storage_text, ha="center", va="center",
            color=PRIMARY, fontsize=9)

    plt.savefig(os.path.join(OUT, "05_script_atomic.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 6. 视频合规证据链
# ─────────────────────────────────────────────────────────
def diagram_06_compliance_chain():
    fig, ax = setup_fig(15, 8)
    ax.text(50, 95, "图10-1  视频合规证据链", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 91, "Source: 全链路加密 + 区块链存证", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # 6 stages pipeline
    stages = [
        ("录制采集",   "音视频 SDK 嵌入\n可信时间戳"),
        ("SM4 加密",   "国密芯片级加密\n密钥隔离"),
        ("OSS 存储",   "对象存储\n分片 + 副本"),
        ("SHA-256 指纹", "哈希校验\n防篡改"),
        ("区块链存证",  "联盟链\n可追溯"),
        ("监管上报",    "银保监接口\n直连"),
    ]
    sw = 13
    sh = 22
    gap = 1.5
    sx = 4
    sy = 60

    for i, (n, d) in enumerate(stages):
        x = sx + i * (sw + gap)
        rect = FancyBboxPatch((x, sy), sw, sh,
                              boxstyle="round,pad=0,rounding_size=0.8",
                              linewidth=1.2, edgecolor=ACCENT, facecolor=ACCENT)
        ax.add_patch(rect)
        ax.text(x + sw/2, sy + sh - 3, n, ha="center", va="center",
                color=WHITE, fontsize=10, fontweight="bold")
        ax.text(x + sw/2, sy + sh/2 - 1, d, ha="center", va="center",
                color=WHITE, fontsize=8)
        if i < len(stages) - 1:
            arrow(ax, x + sw + 0.1, sy + sh/2, x + sw + gap - 0.1, sy + sh/2,
                  color=PRIMARY, width=0.5)

    # Bottom: storage layers
    storage_y = 28
    rect = FancyBboxPatch((4, storage_y), 92, 16,
                          boxstyle="round,pad=0,rounding_size=0.6",
                          linewidth=1, edgecolor=PRIMARY, facecolor=LIGHT)
    ax.add_patch(rect)
    ax.text(50, storage_y + 13, "存储分层", ha="center", va="center",
            color=PRIMARY, fontsize=10, fontweight="bold")
    layers = [
        ("热存储",     "0-90 天",   "OSS 高频访问"),
        ("温存储",     "90 天-3 年","OSS 低频访问"),
        ("冷存储",     "3-10 年",   "归档存储"),
    ]
    lw = 26
    for i, (n, t, d) in enumerate(layers):
        x = 8 + i * 28
        rect = FancyBboxPatch((x, storage_y + 1), lw, 9,
                              boxstyle="round,pad=0,rounding_size=0.4",
                              linewidth=0.6, edgecolor=PRIMARY, facecolor=WHITE)
        ax.add_patch(rect)
        ax.text(x + lw/2, storage_y + 8, n, ha="center", va="center",
                color=PRIMARY, fontsize=9, fontweight="bold")
        ax.text(x + lw/2, storage_y + 5, t, ha="center", va="center",
                color=ACCENT, fontsize=8)
        ax.text(x + lw/2, storage_y + 2, d, ha="center", va="center",
                color=SECONDARY, fontsize=7.5)

    plt.savefig(os.path.join(OUT, "06_compliance_chain.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 7. 智能质检流水线
# ─────────────────────────────────────────────────────────
def diagram_07_quality_pipeline():
    fig, ax = setup_fig(15, 10)
    ax.text(50, 96, "图11-1  智能质检三层流水线", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 92, "Source: L1 规则 + L2 AI + L3 人工 协同质检", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    layers = [
        ("L1 规则层", "0.5s 内", SECONDARY,
         ["100+ 规则模板", "必读项检测", "必答项校验", "必签字校验", "风险关键词"],
         "实时规则匹配 · 高准确率 · 0 漏报"),
        ("L2 AI 智能层", "30s 内", PRIMARY,
         ["ASR 自动转写", "NLP 意图识别", "情感分析", "图像识别(人脸/凭证)", "声纹识别"],
         "深度语义分析 · 多模态融合 · 智能评分"),
        ("L3 人工复核层", "T+1", ACCENT,
         ["高风险 100% 复检", "中风险抽样 30%", "低风险 5% 抽检", "争议件优先处理", "结果可申诉"],
         "兜底保障 · 责任明确 · 申诉通道"),
    ]

    lh = 22
    ly = 65
    for i, (name, time, color, items, desc) in enumerate(layers):
        y = ly - i * (lh + 2)
        # Layer block
        rect = FancyBboxPatch((4, y), 22, lh,
                              boxstyle="round,pad=0,rounding_size=0.8",
                              linewidth=1.5, edgecolor=color, facecolor=color)
        ax.add_patch(rect)
        ax.text(15, y + lh/2 + 3, name, ha="center", va="center",
                color=WHITE, fontsize=13, fontweight="bold")
        ax.text(15, y + lh/2 - 3, time, ha="center", va="center",
                color=WHITE, fontsize=10, fontstyle="italic")
        # Items
        rect = FancyBboxPatch((28, y), 36, lh,
                              boxstyle="round,pad=0,rounding_size=0.6",
                              linewidth=0.8, edgecolor=color, facecolor=LIGHT)
        ax.add_patch(rect)
        for j, item in enumerate(items):
            col = j % 2
            row = j // 2
            ix = 30 + col * 16
            iy = y + lh - 4 - row * 4.5
            ax.text(ix, iy, "· " + item, ha="left", va="center",
                    color=PRIMARY, fontsize=9)
        # Description
        rect = FancyBboxPatch((66, y), 30, lh,
                              boxstyle="round,pad=0,rounding_size=0.6",
                              linewidth=0.8, edgecolor=color, facecolor=WHITE)
        ax.add_patch(rect)
        ax.text(81, y + lh/2, desc, ha="center", va="center",
                color=PRIMARY, fontsize=9.5)

    # Data flow arrows between layers
    for i in range(2):
        y1 = ly - i * (lh + 2)
        y2 = ly - (i + 1) * (lh + 2) + lh
        arrow(ax, 15, y1, 15, y2, color=ACCENT, width=0.5)
        ax.text(17, (y1 + y2) / 2, "升级", ha="left", va="center",
                color=ACCENT, fontsize=8, fontweight="bold")

    plt.savefig(os.path.join(OUT, "07_quality_pipeline.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 8. 异常处理分类图
# ─────────────────────────────────────────────────────────
def diagram_08_exception_handling():
    fig, ax = setup_fig(15, 10)
    ax.text(50, 95, "图8-2  异常处理分类与恢复", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 91, "Source: 三类异常 + 客户保护机制", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # 3 categories
    cats = [
        ("技术异常", DANGER, ["网络断线", "设备故障", "音视频异常", "OSS 上传失败"],
         "本地缓存 + 断点续传 + 自动告警"),
        ("流程异常", WARNING, ["客户拒答", "话术中断", "中途离席", "二次确认失败"],
         "重新进入该节点 + 客户经理介入"),
        ("合规异常", ACCENT, ["风险不匹配", "证件失效", "非本人办理", "高风险反洗钱"],
         "阻断流程 + 升级审核 + 留痕上报"),
    ]
    cw = 28
    ch = 36
    cy = 50
    for i, (name, color, items, action) in enumerate(cats):
        x = 4 + i * (cw + 2)
        rect = FancyBboxPatch((x, cy), cw, ch,
                              boxstyle="round,pad=0,rounding_size=0.8",
                              linewidth=1.2, edgecolor=color, facecolor=LIGHT)
        ax.add_patch(rect)
        # Header
        rect = Rectangle((x, cy + ch - 6), cw, 6, facecolor=color, edgecolor="none")
        ax.add_patch(rect)
        ax.text(x + cw/2, cy + ch - 3, name, ha="center", va="center",
                color=WHITE, fontsize=13, fontweight="bold")
        # Items
        for j, item in enumerate(items):
            ax.text(x + cw/2, cy + ch - 11 - j * 4, "· " + item, ha="center", va="center",
                    color=PRIMARY, fontsize=10)
        # Action
        rect = FancyBboxPatch((x + 1, cy + 2), cw - 2, 9,
                              boxstyle="round,pad=0,rounding_size=0.4",
                              linewidth=0.8, edgecolor=color, facecolor=color)
        ax.add_patch(rect)
        ax.text(x + cw/2, cy + 6.5, action, ha="center", va="center",
                color=WHITE, fontsize=9, fontweight="bold")

    # Customer protection
    cp_y = 14
    rect = FancyBboxPatch((4, cp_y), 92, 22,
                          boxstyle="round,pad=0,rounding_size=0.8",
                          linewidth=1.5, edgecolor=PRIMARY, facecolor=PRIMARY)
    ax.add_patch(rect)
    ax.text(50, cp_y + 19, "客户保护机制 (6 重保障)", ha="center", va="center",
            color=WHITE, fontsize=12, fontweight="bold")

    protections = [
        ("多端续接",   "线上中断 → 线下去办"),
        ("暂存恢复",   "本地缓存 24h"),
        ("远程协助",   "客户经理接管"),
        ("二次预约",   "7 天内可重约"),
        ("人工兜底",   "955xx 坐席"),
        ("短信进度",   "每节点通知客户"),
    ]
    pw = 14
    for i, (n, d) in enumerate(protections):
        x = 7 + i * 14.5
        rect = FancyBboxPatch((x, cp_y + 3), pw, 12,
                              boxstyle="round,pad=0,rounding_size=0.5",
                              linewidth=0.8, edgecolor=ACCENT, facecolor=ACCENT)
        ax.add_patch(rect)
        ax.text(x + pw/2, cp_y + 11, n, ha="center", va="center",
                color=WHITE, fontsize=10, fontweight="bold")
        ax.text(x + pw/2, cp_y + 5, d, ha="center", va="center",
                color=WHITE, fontsize=8)

    plt.savefig(os.path.join(OUT, "08_exception_handling.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 9. 部署架构图
# ─────────────────────────────────────────────────────────
def diagram_09_deployment():
    fig, ax = setup_fig(16, 10)
    ax.text(50, 95, "图3-2  系统部署架构 (3 个逻辑区 + 9 个核心服务)", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 91, "Source: 生产/沙箱/办公 三网隔离 + 高可用", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # 3 zones
    zones = [
        ("DMZ 区 (公网)", ACCENT, 4, 70, 26, 17,
         ["Nginx/WAF", "API 网关", "线上 H5"]),
        ("生产区 (内网)", PRIMARY, 33, 70, 38, 17,
         ["流程引擎", "话术中心", "双录服务", "质检引擎", "电子签", "风评", "订单中心"]),
        ("办公区 (内网)", SECONDARY, 74, 70, 22, 17,
         ["CRM 接入", "客户经理", "PAD 同步"]),
    ]
    for name, color, x, y, w, h, services in zones:
        rect = FancyBboxPatch((x, y), w, h,
                              boxstyle="round,pad=0,rounding_size=0.6",
                              linewidth=1.5, edgecolor=color, facecolor=WHITE)
        ax.add_patch(rect)
        rect = Rectangle((x, y + h - 3), w, 3, facecolor=color, edgecolor="none")
        ax.add_patch(rect)
        ax.text(x + w/2, y + h - 1.5, name, ha="center", va="center",
                color=WHITE, fontsize=11, fontweight="bold")
        for i, s in enumerate(services):
            col = i % 3
            row = i // 3
            sx = x + 1.5 + col * ((w - 3) / 3)
            sy = y + h - 6 - row * 3.5
            rect = Rectangle((sx, sy - 1.5), (w - 3) / 3 - 0.5, 2.5,
                             facecolor=LIGHT, edgecolor=SECONDARY, linewidth=0.5)
            ax.add_patch(rect)
            ax.text(sx + (w - 3) / 6, sy - 0.25, s, ha="center", va="center",
                    color=PRIMARY, fontsize=8)

    # Bottom: data layer
    dy = 38
    rect = FancyBboxPatch((8, dy), 88, 26,
                          boxstyle="round,pad=0,rounding_size=0.6",
                          linewidth=1.5, edgecolor=PRIMARY, facecolor=PRIMARY)
    ax.add_patch(rect)
    ax.text(52, dy + 23, "数据层 (强一致 + 高可用)", ha="center", va="center",
            color=WHITE, fontsize=11, fontweight="bold")
    data = [
        ("MySQL 8.0 集群",    "主从 + MGR", "订单/客户/话术"),
        ("Redis Cluster",      "6 节点分片",  "会话/缓存/锁"),
        ("OSS 对象存储",       "3 副本 EC",   "音视频/凭证"),
        ("Elasticsearch",      "3 主 3 从",   "质检日志/检索"),
        ("Kafka 消息队列",     "3 broker",   "异步事件"),
        ("区块链 (Hyperledger)", "4 节点共识", "存证/监管"),
    ]
    dw = 14
    for i, (n, d, p) in enumerate(data):
        col = i % 6
        x = 9 + col * 14.5
        rect = FancyBboxPatch((x, dy + 3), dw, 18,
                              boxstyle="round,pad=0,rounding_size=0.4",
                              linewidth=0.6, edgecolor=ACCENT, facecolor=LIGHT)
        ax.add_patch(rect)
        ax.text(x + dw/2, dy + 17, n, ha="center", va="center",
                color=PRIMARY, fontsize=8.5, fontweight="bold")
        ax.text(x + dw/2, dy + 12, d, ha="center", va="center",
                color=ACCENT, fontsize=7.5)
        ax.text(x + dw/2, dy + 6, p, ha="center", va="center",
                color=SECONDARY, fontsize=7.5)

    # Network callout
    nc_y = 8
    rect = FancyBboxPatch((8, nc_y), 88, 18,
                          boxstyle="round,pad=0,rounding_size=0.5",
                          linewidth=1, edgecolor=PRIMARY, facecolor=WHITE)
    ax.add_patch(rect)
    ax.text(52, nc_y + 15, "网络与安全", ha="center", va="center",
            color=PRIMARY, fontsize=11, fontweight="bold")
    nets = [
        ("国密 SM2/SM4 加密", "所有通信"),
        ("HTTPS / mTLS",      "API + 消息"),
        ("VPC 网络隔离",      "DMZ/生产/办公"),
        ("WAF + 入侵检测",    "边界防护"),
        ("审计日志 (WORM)",   "180 天留存"),
    ]
    nw = 16
    for i, (n, d) in enumerate(nets):
        x = 10 + i * 17
        ax.text(x + nw/2, nc_y + 9, n, ha="center", va="center",
                color=PRIMARY, fontsize=9, fontweight="bold")
        ax.text(x + nw/2, nc_y + 4, d, ha="center", va="center",
                color=SECONDARY, fontsize=8)

    plt.savefig(os.path.join(OUT, "09_deployment.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()

# ─────────────────────────────────────────────────────────
# 10. 数据模型 ER 图
# ─────────────────────────────────────────────────────────
def diagram_10_er():
    fig, ax = setup_fig(16, 11)
    ax.text(50, 96, "图7-3  核心数据模型 ER 图", ha="center", va="center",
            color=PRIMARY, fontsize=14, fontweight="bold")
    ax.text(50, 92, "Source: 7 个核心实体 + 主外键关系", ha="center", va="center",
            color=SECONDARY, fontsize=8, fontstyle="italic")

    # 7 entities
    entities = [
        ("客户\n(Customer)", 8, 70,
         [("customer_id", "PK"), ("name", ""), ("id_no", ""), ("risk_level", "")]),
        ("订单\n(Order)",    32, 70,
         [("order_id", "PK"), ("customer_id", "FK"), ("product_id", "FK"), ("state", ""), ("created_at", "")]),
        ("双录会话\n(Session)", 56, 70,
         [("session_id", "PK"), ("order_id", "FK"), ("channel", ""), ("video_url", ""), ("state", "")]),
        ("话术\n(Script)",   80, 70,
         [("script_id", "PK"), ("version", ""), ("content", "JSON"), ("product_type", "")]),
        ("风评\n(RiskAssess)", 8, 30,
         [("assess_id", "PK"), ("customer_id", "FK"), ("order_id", "FK"), ("risk_level", ""), ("answers", "JSON")]),
        ("质检\n(Quality)",  32, 30,
         [("qa_id", "PK"), ("session_id", "FK"), ("score", ""), ("verdict", ""), ("issues", "JSON")]),
        ("电子合同\n(Contract)", 56, 30,
         [("contract_id", "PK"), ("order_id", "FK"), ("sign_ca", ""), ("hash", ""), ("signed_at", "")]),
    ]
    for name, x, y, fields in entities:
        w = 16
        h = 5 + len(fields) * 3
        rect = FancyBboxPatch((x, y), w, h,
                              boxstyle="round,pad=0,rounding_size=0.4",
                              linewidth=1.2, edgecolor=PRIMARY, facecolor=LIGHT)
        ax.add_patch(rect)
        # Header
        rect = Rectangle((x, y + h - 4), w, 4, facecolor=PRIMARY, edgecolor="none")
        ax.add_patch(rect)
        ax.text(x + w/2, y + h - 2, name, ha="center", va="center",
                color=WHITE, fontsize=10, fontweight="bold")
        # Fields
        for i, (fname, ftype) in enumerate(fields):
            fy = y + h - 5 - i * 2.7
            ax.text(x + 0.5, fy, fname, ha="left", va="center",
                    color=PRIMARY, fontsize=8.5)
            color = ACCENT if ftype == "PK" else (SUCCESS if ftype == "FK" else SECONDARY)
            ax.text(x + w - 0.5, fy, ftype, ha="right", va="center",
                    color=color, fontsize=8, fontweight="bold")

    # Relations (lines)
    relations = [
        # customer -> order
        ((8 + 8, 70 + 12), (32, 70 + 12)),
        # order -> session
        ((32 + 8, 70 + 8), (56, 70 + 8)),
        # script -> session
        ((80 + 8, 70 + 12), (56 + 8, 70 + 8)),
        # order -> risk
        ((32 + 4, 70), (8 + 4, 30 + 15)),
        # order -> quality
        ((32 + 8, 70), (32 + 8, 30 + 15)),
        # order -> contract
        ((32 + 12, 70), (56 + 4, 30 + 15)),
    ]
    for (x1, y1), (x2, y2) in relations:
        arrow(ax, x1, y1, x2, y2, color=ACCENT, width=0.3, style="->")

    # Legend
    leg_y = 8
    ax.text(10, leg_y + 4, "PK", color=ACCENT, fontsize=10, fontweight="bold")
    ax.text(13, leg_y + 4, "主键", color=PRIMARY, fontsize=9)
    ax.text(20, leg_y + 4, "FK", color=SUCCESS, fontsize=10, fontweight="bold")
    ax.text(23, leg_y + 4, "外键", color=PRIMARY, fontsize=9)
    ax.text(50, leg_y + 4, "JSON", color=SECONDARY, fontsize=9)
    ax.text(56, leg_y + 4, "JSON 字段", color=PRIMARY, fontsize=9)

    plt.savefig(os.path.join(OUT, "10_data_model.png"),
                bbox_inches="tight", dpi=180, facecolor=WHITE)
    plt.close()


if __name__ == "__main__":
    print("Generating diagrams...")
    diagram_01_overall_architecture()
    print("  01_overall_architecture.png")
    diagram_02_end_to_end_flow()
    print("  02_end_to_end_flow.png")
    diagram_03_state_machine()
    print("  03_state_machine.png")
    diagram_04_saga()
    print("  04_saga_transaction.png")
    diagram_05_script_atomic()
    print("  05_script_atomic.png")
    diagram_06_compliance_chain()
    print("  06_compliance_chain.png")
    diagram_07_quality_pipeline()
    print("  07_quality_pipeline.png")
    diagram_08_exception_handling()
    print("  08_exception_handling.png")
    diagram_09_deployment()
    print("  09_deployment.png")
    diagram_10_er()
    print("  10_data_model.png")
    print("Done.")
