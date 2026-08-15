#!/bin/bash
# V6.3+ i18n 翻译完成度 (V8 - 不 eval, 文本匹配)
set -e
cd "$(dirname "$0")/.."

cd frontend
node << 'NODEEOF'
const fs = require('fs');
const path = require('path');

// 提取 zh.js / en.js 的所有 'key': 'value' 模式 (含 'admin.menu.dashboard' 嵌套)
function extractKeys(filepath) {
  if (!fs.existsSync(filepath)) return new Set();
  const text = fs.readFileSync(filepath, 'utf8');
  const keys = new Set();
  // 1. 找 'key' : 'value' (单层)
  for (const m of text.matchAll(/['"]([a-zA-Z][a-zA-Z0-9_]*)['"]\s*:\s*['"][^'"]+['"]/g)) {
    keys.add(m[1]);
  }
  // 2. 找 'key.xxx' : 'value' (嵌套)
  for (const m of text.matchAll(/['"]([a-zA-Z][a-zA-Z0-9_.]+)['"]\s*:\s*['"][^'"]+['"]/g)) {
    keys.add(m[1]);
  }
  return keys;
}

const zhKeys = extractKeys('src/i18n/locales/zh.js');
const enKeys = extractKeys('src/i18n/locales/en.js');

// 收集前端用的 key
const used = new Set();
const re = /(?<![$\w])t\(['"`]([a-zA-Z][a-zA-Z0-9._]+)['"`]/g;
const re2 = /\$t\(['"`]([a-zA-Z][a-zA-Z0-9._]+)['"`]/g;

function walk(dir) {
  for (const f of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, f.name);
    if (f.isDirectory()) walk(p);
    else if (f.name.endsWith('.vue') || f.name.endsWith('.js')) {
      const text = fs.readFileSync(p, 'utf8');
      let m;
      while ((m = re.exec(text))) used.add(m[1]);
      re.lastIndex = 0;
      while ((m = re2.exec(text))) used.add(m[1]);
      re2.lastIndex = 0;
    }
  }
}
walk('src');

console.log('=========================================');
console.log('V6.3+ i18n 翻译完成度 (V8)');
console.log('=========================================');
console.log('前端 t(\'xxx\') 用的 key:  ' + used.size);
console.log('zh.js 有 key:           ' + zhKeys.size);
console.log('en.js 有 key:           ' + enKeys.size);
console.log('');

const zhHave = [...used].filter(k => zhKeys.has(k));
const enHave = [...used].filter(k => enKeys.has(k));
console.log('中文覆盖度: ' + (zhHave.length * 100 / used.size).toFixed(1) + '% (' + zhHave.length + '/' + used.size + ')');
console.log('英文覆盖度: ' + (enHave.length * 100 / used.size).toFixed(1) + '% (' + enHave.length + '/' + used.size + ')');

const missingZh = [...used].filter(k => !zhKeys.has(k));
const missingEn = [...used].filter(k => !enKeys.has(k));

if (missingZh.length) {
  console.log('');
  console.log('zh.js 缺失 ' + missingZh.length + ' 个:');
  missingZh.slice(0, 20).forEach(k => console.log('  - ' + k));
  if (missingZh.length > 20) console.log('  ... +' + (missingZh.length - 20));
}
if (missingEn.length) {
  console.log('');
  console.log('en.js 缺失 ' + missingEn.length + ' 个:');
  missingEn.slice(0, 20).forEach(k => console.log('  - ' + k));
  if (missingEn.length > 20) console.log('  ... +' + (missingEn.length - 20));
}
console.log('=========================================');

// 输出 JSON 给后续补全脚本
fs.writeFileSync('/tmp/missing-keys.json', JSON.stringify({
  zh: missingZh, en: missingEn
}, null, 2));
console.log('');
console.log('📝 写到 /tmp/missing-keys.json');
NODEEOF
