#!/usr/bin/env python3
"""
从 git HEAD 提取每个模块的特殊 minimax.* 配置
合并到重构后的 application.yml
并补充 datasource/mybatis-plus (从 common yml 拆出来后的)
"""
import subprocess
import re
import sys
from pathlib import Path

ROOT = Path("/workspace/minimax-platform")
modules = ['auth', 'chat', 'model', 'memory', 'rag', 'function', 'admin',
           'multimodal', 'monitor', 'agent', 'prompt', 'ws']

# 各业务模块 (不含 gateway) 通用配置
BUSINESS_COMMON = """
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:3306/minimax_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:minimax}
    password: ${MYSQL_PASSWORD:minimax_pass_2024}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30s
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASS:minimax_redis_2024}
      database: 0
      timeout: 3s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 1
  mybatis-plus:
    configuration:
      map-underscore-to-camel-case: true
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    global-config:
      db-config:
        id-type: ASSIGN_ID
        logic-delete-field: deleted
        logic-delete-value: 1
        logic-not-delete-value: 0
    mapper-locations: classpath*:mapper/**/*.xml
"""

for m in modules:
    yml = ROOT / f"backend/minimax-{m}/src/main/resources/application.yml"
    if not yml.exists():
        print(f"  ❌ {m} (no yml)")
        continue

    current = yml.read_text()

    # 1. 如果没有 datasource, 加进去
    if 'datasource:' not in current and 'spring.config.import' in current:
        # 在 application: name 之前加
        marker = '  application:\n    name:'
        if marker in current:
            new_content = current.replace(
                marker,
                BUSINESS_COMMON + marker,
                1
            )
            if new_content != current:
                yml.write_text(new_content)
                current = new_content
                print(f"  ✓ {m} (added datasource+redis+mybatis-plus)")

    # 2. 从 git HEAD 恢复 minimax 块 (如果有)
    result = subprocess.run(
        ['git', 'show', f'HEAD:backend/minimax-{m}/src/main/resources/application.yml'],
        cwd=ROOT, capture_output=True, text=True
    )
    if result.returncode != 0:
        continue

    orig = result.stdout
    match = re.search(r'^minimax:.*?(?=^\S|\Z)', orig, re.MULTILINE | re.DOTALL)
    if not match:
        continue

    custom_block = match.group(0)
    lines = custom_block.split('\n')
    new_lines = []
    for i, line in enumerate(lines):
        if i == 0 and line == "minimax:":
            continue
        if line.startswith('  '):
            new_lines.append(line[2:])
        else:
            new_lines.append(line)
    custom = '\n'.join(new_lines).rstrip()

    if not custom:
        continue

    placeholder = "minimax:\n  # (无特殊配置)"
    new_block = f"minimax:\n{custom}\n"

    if placeholder in current:
        yml.write_text(current.replace(placeholder, new_block))
        print(f"  ✓ {m} (replaced minimax placeholder)")
    elif "minimax:" not in current or current.count('minimax:') == 1:
        # 没有 minimax 块
        if not current.endswith('\n'):
            current += '\n'
        yml.write_text(current + f"\n# 模块特殊配置 (从 git HEAD 恢复)\n{new_block}")
        print(f"  ✓ {m} (appended minimax)")

print("\n=== 验证 ===")
for m in modules:
    yml = ROOT / f"backend/minimax-{m}/src/main/resources/application.yml"
    if yml.exists():
        content = yml.read_text()
        has_ds = 'datasource:' in content
        has_minimax = 'minimax:' in content
        print(f"  {m}: datasource={has_ds}  minimax={has_minimax}")