#!/usr/bin/env python3
"""
Convert openapi.yaml to a human-readable Markdown API reference.
Usage: python3 openapi-to-md.py
"""
import yaml
import sys
import textwrap
from pathlib import Path

OPENAPI_PATH = Path('/workspace/minimax-platform/docs/openapi.yaml')
OUTPUT_PATH = Path('/workspace/minimax-platform/docs/API.md')


def method_badge(m: str) -> str:
    colors = {'get': '4CAF50', 'post': '2196F3', 'put': 'FF9800',
              'delete': 'F44336', 'patch': '9C27B0'}
    c = colors.get(m.lower(), '607D8B')
    return f'<span style="background:#{c};color:#fff;padding:2px 8px;border-radius:4px;font-size:12px">{m.upper()}</span>'


def format_schema(schema: dict, depth=0) -> str:
    """Render a schema as a concise Markdown table or text."""
    if not schema:
        return ''
    if '$ref' in schema:
        ref = schema['$ref'].split('/')[-1]
        return f'`{ref}`'
    typ = schema.get('type', 'object')
    if typ == 'array':
        items = schema.get('items', {})
        return 'array of ' + format_schema(items, depth+1)
    if typ == 'object':
        props = schema.get('properties', {})
        if not props:
            return 'object'
        rows = []
        for k, v in props.items():
            required = '**' if k in schema.get('required', []) else ''
            rows.append(f'| {required}{k}{required} | {v.get("type","")} | {v.get("description","")} |')
        header = '| 参数 | 类型 | 说明 |\n|---|---|---|'
        return header + '\n' + '\n'.join(rows)
    desc = schema.get('description', '')
    enum = schema.get('enum', [])
    enum_str = f'（枚举: {enum})' if enum else ''
    default = f' 默认: `{schema.get("default")}`' if 'default' in schema else ''
    return f'{typ}{enum_str}{default}  — {desc}' if desc else typ


def format_request_body(body: dict) -> str:
    """Format request body with schema."""
    content = body.get('content', {})
    parts = []
    for media, spec in content.items():
        schema = spec.get('schema', {})
        desc = body.get('description', '')
        parts.append(f'**Content-Type:** `{media}`\n')
        if desc:
            parts.append(f'{desc}\n')
        parts.append(format_schema(schema) + '\n')
    return '\n'.join(parts)


def main():
    spec = yaml.safe_load(OPENAPI_PATH.read_text())

    lines = []
    # Header
    lines.append('# MiniMax 平台 API 参考文档\n')
    lines.append('> 由 `openapi.yaml` 自动生成 · 请勿手动修改\n')
    info = spec.get('info', {})
    lines.append(f"**版本:** {info.get('version', 'N/A')}  \n")
    lines.append(f"**描述:** {info.get('description', '')}  \n")
    if servers := spec.get('servers', []):
        base = servers[0].get('url', '')
        lines.append(f"**Base URL:** `{base}`  \n")
    lines.append('\n---\n')

    # TOC
    lines.append('## 目录\n')
    tags = spec.get('tags', [])
    paths = spec.get('paths', {})

    tag_map = {}
    for t in tags:
        tag_map[t['name']] = t.get('description', '')

    for t in tags:
        name = t['name']
        lines.append(f'- [{name}](#{name.lower().replace(" ", "-")})')
        for path, methods in paths.items():
            for method, op in methods.items():
                if name in op.get('tags', []):
                    lines.append(f'  - [{method.upper()} {path}](#{method}{path.replace("/", "-")})')
    lines.append('\n---\n')

    # Paths
    for path, methods in sorted(paths.items()):
        for method, op in sorted(methods.items()):
            if method not in ('get', 'post', 'put', 'delete', 'patch'):
                continue

            tags_list = op.get('tags', [''])
            tag = tags_list[0]
            summary = op.get('summary', '')
            desc = op.get('description', '') or op.get('summary', '')
            operation_id = op.get('operationId', '')
            deprecated = '**⚠️ 已废弃**\n' if op.get('deprecated') else ''

            anchor = f'{method}{path.replace("/","-")}'
            lines.append(f'### {method_badge(method)} `{path}`\n')
            if summary:
                lines.append(f'**{summary}**\n')
            if deprecated:
                lines.append(deprecated)
            if desc and desc != summary:
                lines.append(f'{desc}\n')
            if operation_id:
                lines.append(f'*operationId: `{operation_id}`*\n')

            # Parameters
            params = op.get('parameters', [])
            if params:
                lines.append('\n**Query / Path 参数**\n')
                lines.append('| 参数名 | 类型 | 必填 | 说明 |\n|---|---|---|---|\n')
                for p in params:
                    lines.append(f"| `{p.get('name','')}` | {p.get('schema',{}).get('type','string')} | {'是' if p.get('required') else '否'} | {p.get('description','')} |\n")

            # Request body
            req_body = op.get('requestBody', {})
            if req_body:
                lines.append('\n**请求体 (Request Body)**\n')
                lines.append(format_request_body(req_body))

            # Responses
            responses = op.get('responses', {})
            if responses:
                lines.append('\n**响应**\n')
                for code, resp in sorted(responses.items()):
                    desc = resp.get('description', '')
                    content = resp.get('content', {})
                    schema_info = ''
                    for media, spec in content.items():
                        schema = spec.get('schema', {})
                        if schema:
                            schema_info = f' → {format_schema(schema)}'
                    lines.append(f'- `{code}` {desc}{schema_info}\n')

            lines.append('\n---\n')

    # Schemas
    components = spec.get('components', {})
    schemas = components.get('schemas', {})
    if schemas:
        lines.append('\n## 数据模型 (Schemas)\n\n')
        for name, schema in sorted(schemas.items()):
            if '$ref' in schema:
                continue
            lines.append(f'### `{name}`\n')
            if desc := schema.get('description'):
                lines.append(f'{desc}\n')
            props = schema.get('properties', {})
            required = schema.get('required', [])
            if props:
                lines.append('| 字段 | 类型 | 必填 | 说明 |\n|---|---|---|---|\n')
                for k, v in sorted(props.items()):
                    req = '**是**' if k in required else '否'
                    lines.append(f"| `{k}` | {v.get('type','object')} | {req} | {v.get('description','')} |\n")

    OUTPUT_PATH.write_text('\n'.join(lines))
    print(f'Generated: {OUTPUT_PATH} ({len(lines)} lines)')


if __name__ == '__main__':
    main()
