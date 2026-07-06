#!/usr/bin/env node
/**
 * openapi.yaml → Markdown API 文档生成器 (Day 25)
 * 输出: docs/API.md
 */
const fs = require('fs');
const yaml = require('yaml');

const spec = yaml.parse(fs.readFileSync('docs/openapi.yaml', 'utf8'));

// Group paths by first segment
const groups = {};
Object.entries(spec.paths || {}).forEach(([path, methods]) => {
  const seg = path.split('/').filter(Boolean)[0] || 'root';
  if (!groups[seg]) groups[seg] = {};
  groups[seg][path] = methods;
});

const order = ['auth', 'chat', 'sessions', 'messages', 'models', 'memory', 'rag', 'function', 'agent', 'admin', 'monitor', 'multimodal', 'prompts', 'pipeline', 'root'];
const sorted = Object.keys(groups).sort((a, b) => {
  const ai = order.indexOf(a), bi = order.indexOf(b);
  return (ai === -1 ? 99 : ai) - (bi === -1 ? 99 : bi);
});

let md = '';
md += '# MiniMax AI Platform — API 文档\n\n';
md += `> **版本**: ${spec.info.version} | **Base**: ${spec.servers[0].url}\n`;
md += '> **认证**: `Authorization: Bearer <accessToken>`（公开端点除外）\n\n';

md += '## 统一响应格式\n\n';
md += '```json\n';
md += '{\n';
md += '  "code": 0,\n';
md += '  "message": "OK",\n';
md += '  "data": { ... },\n';
md += '  "timestamp": 1234567890\n';
md += '}\n';
md += '```\n\n';

md += '## 错误码\n\n';
md += '| code | 说明 |\n|------|------|\n';
md += '| 0 | 成功 |\n';
md += '| 1000 | 业务异常 |\n';
md += '| 1001 | 参数校验失败 |\n';
md += '| 1002 | 未登录 |\n';
md += '| 1003 | 无权限 |\n';
md += '| 1500 | 服务降级/限流 |\n\n';
md += '---\n\n';

const METHOD_COLOR = { get: 'GET', post: 'POST', put: 'PUT', delete: 'DELETE', patch: 'PATCH' };

sorted.forEach(group => {
  const paths = groups[group];
  const title = group === 'root' ? '其他' : '/' + group;
  md += `## ${title}\n\n`;

  Object.entries(paths).sort().forEach(([path, methods]) => {
    Object.entries(methods).forEach(([method, op]) => {
      if (!op || typeof op !== 'object') return;
      const summary = op.summary || op.description || '';
      const params = op.parameters || [];
      const body = op.requestBody;
      const responses = op.responses || {};
      const operationId = op.operationId || '';
      const tags = (op.tags || []).join(' / ');

      md += `### ${method.toUpperCase()} ${path}\n\n`;
      if (tags) md += `> 标签: ${tags}\n\n`;
      if (summary) md += `${summary}\n\n`;
      if (operationId) md += `> operationId: \`${operationId}\`\n\n`;

      // Parameters
      if (params.length > 0) {
        md += '**Query/Path 参数**:\n\n';
        md += '| 名称 | 位置 | 类型 | 必填 | 说明 |\n';
        md += '|------|------|------|------|------|\n';
        params.forEach(p => {
          const type = p.schema ? (p.schema.type + (p.schema.format ? `(${p.schema.format})` : '')) : 'string';
          md += `| \`${p.name}\` | ${p.in || 'query'} | ${type} | ${p.required ? '✅' : '❌'} | ${p.description || '-'} |\n`;
        });
        md += '\n';
      }

      // Request body
      if (body) {
        md += '**请求体**:\n\n';
        const contentType = Object.keys(body.content || {})[0] || 'application/json';
        md += `Content-Type: \`${contentType}\`\n\n`;
        if (body.description) md += `${body.description}\n\n`;
        const schemaRef = body.content?.[contentType]?.schema;
        if (schemaRef) {
          const example = body.content?.[contentType]?.example || schemaRef.example;
          if (example) {
            md += '```json\n';
            md += JSON.stringify(example, null, 2) + '\n';
            md += '```\n\n';
          } else if (schemaRef.properties) {
            md += '**字段**:\n\n';
            md += '| 字段 | 类型 | 说明 |\n';
            md += '|------|------|------|\n';
            Object.entries(schemaRef.properties).forEach(([k, v]) => {
              md += `| \`${k}\` | ${v.type || 'object'} | ${v.description || '-'} |\n`;
            });
            md += '\n';
          }
        }
      }

      // Responses
      const firstCode = Object.keys(responses)[0] || '200';
      const firstResp = responses[firstCode] || {};
      const respContent = firstResp.content ? firstResp.content['application/json'] : null;
      const respSchema = respContent?.schema;
      if (respSchema) {
        const respExample = respContent?.example || respSchema.example;
        md += `**响应** (${firstCode}${firstResp.description ? ' ' + firstResp.description : ''}):\n\n`;
        if (respExample) {
          md += '```json\n';
          md += JSON.stringify(respExample, null, 2) + '\n';
          md += '```\n\n';
        } else if (respSchema.properties) {
          md += '| 字段 | 类型 | 说明 |\n';
          md += '|------|------|------|\n';
          Object.entries(respSchema.properties).forEach(([k, v]) => {
            md += `| \`${k}\` | ${v.type || 'object'} | ${v.description || '-'} |\n`;
          });
          md += '\n';
        }
      }
    });
  });
});

// Security
md += '---\n\n';
md += '## 认证方式\n\n';
md += '使用 **JWT Bearer Token**:\n\n';
md += '```bash\n';
md += `curl -H "Authorization: Bearer <accessToken>" ${spec.servers[0].url}/api/v1/...\n`;
md += '```\n\n';

// Base URLs
md += '## 服务 Base URL\n\n';
md += '| 服务 | Port | Base |\n';
md += '|------|------|------|\n';
md += '| API Gateway | 7080 | /api/v1 |\n';
md += '| Auth | 8081 | / |\n';
md += '| Chat | 8082 | / |\n';
md += '| Model | 8083 | / |\n';
md += '| Memory | 8084 | / |\n';
md += '| RAG | 8085 | / |\n';
md += '| Function | 8086 | / |\n';
md += '| Admin | 8087 | / |\n';
md += '| Monitor | 8089 | / |\n';
md += '| Multimodal | 8088 | / |\n';
md += '| Agent | 8090 | / |\n';
md += '| Prompt | 8091 | / |\n\n';

md += '---\n\n';
md += `*由 openapi.yaml 自动生成 | ${new Date().toISOString().split('T')[0]}*\n`;

fs.writeFileSync('docs/API.md', md, 'utf8');
const stats = fs.statSync('docs/API.md');
console.log('✅ docs/API.md generated: ' + stats.size + ' bytes');
