#!/usr/bin/env python3
"""
ONNX 测试模型生成器 (V3.1.0)

生成用于 JUnit 测试的最小 ONNX 模型:
  1. add_one.onnx      y = x + 1.0, inputs=[1,3] float
  2. simple_classify.onnx  3 类分类器 (随机权重, 验证输出维度)

依赖:
  pip install onnx onnxruntime

输出:
  scripts/onnx-test/add_one.onnx
  scripts/onnx-test/simple_classify.onnx

用法:
  python3 scripts/gen_onnx_test_model.py
"""
import os
import numpy as np
import onnx
from onnx import helper, TensorProto

# 1. 输出目录
OUT_DIR = os.path.join(os.path.dirname(__file__), "onnx-test")
os.makedirs(OUT_DIR, exist_ok=True)

# ============ 1. add_one.onnx ============

# 输入: x [1, 3] float
x_input = helper.make_tensor_value_info("x", TensorProto.FLOAT, [1, 3])
# 输出: y [1, 3] float
y_output = helper.make_tensor_value_info("y", TensorProto.FLOAT, [1, 3])

# 常量: ones [1, 3] float = 1.0
ones_init = helper.make_tensor(
    name="ones",
    data_type=TensorProto.FLOAT,
    dims=[1, 3],
    vals=np.ones((1, 3), dtype=np.float32).flatten().tobytes(),
    raw=True
)

# 节点: y = x + ones
add_node = helper.make_node(
    "Add",
    inputs=["x", "ones"],
    outputs=["y"]
)

# 构造图
add_graph = helper.make_graph(
    nodes=[add_node],
    name="AddOneGraph",
    inputs=[x_input],
    outputs=[y_output],
    initializer=[ones_init]
)

add_model = helper.make_model(add_graph, producer_name="minimax-test")
add_model.opset_import[0].version = 13
add_path = os.path.join(OUT_DIR, "add_one.onnx")
onnx.save(add_model, add_path)
print(f"[gen] add_one.onnx -> {add_path} ({os.path.getsize(add_path)} bytes)")

# ============ 2. simple_classify.onnx ============

# 分类器: 3 输入 -> 5 类
# 权重: 随机生成 (3x5), bias: 零
np.random.seed(42)
W = np.random.randn(3, 5).astype(np.float32)
B = np.zeros((1, 5), dtype=np.float32)

w_init = helper.make_tensor("W", TensorProto.FLOAT, [3, 5], W.flatten().tolist())
b_init = helper.make_tensor("B", TensorProto.FLOAT, [1, 5], B.flatten().tolist())

c_input = helper.make_tensor_value_info("input", TensorProto.FLOAT, [1, 3])
c_output = helper.make_tensor_value_info("logits", TensorProto.FLOAT, [1, 5])

# MatMul: input @ W = [1, 5]
matmul_node = helper.make_node("MatMul", ["input", "W"], ["mm_out"])
# Add: + B
add2_node = helper.make_node("Add", ["mm_out", "B"], ["logits"])

c_graph = helper.make_graph(
    nodes=[matmul_node, add2_node],
    name="SimpleClassifier",
    inputs=[c_input],
    outputs=[c_output],
    initializer=[w_init, b_init]
)

c_model = helper.make_model(c_graph, producer_name="minimax-test")
c_model.opset_import[0].version = 13
c_path = os.path.join(OUT_DIR, "simple_classify.onnx")
onnx.save(c_model, c_path)
print(f"[gen] simple_classify.onnx -> {c_path} ({os.path.getsize(c_path)} bytes)")

print("[gen] 完成")
