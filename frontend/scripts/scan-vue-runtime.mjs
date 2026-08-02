/**
 * 模拟 Vue 渲染, 抓 _ctx.X is not a function / X is undefined
 */
import { parse, compileScript, compileTemplate, compileStyle } from 'vue/compiler-sfc'
import { createSSRApp, h, defineComponent } from 'vue'
import { renderToString } from 'vue/server-renderer'
import fs from 'node:fs'
import path from 'node:path'

function* walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules') continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p)
    else if (e.name.endsWith('.vue')) yield p
  }
}

// 简单 eval script (在受控 context)
function evalScript(script, filename) {
  try {
    // wrap 成 IIFE, 收集 export
    const wrapped = `
      const __exports = {};
      const __module = { exports: __exports };
      ${script.replace(/export\s+default\s+/, '__module.exports = ')}
      ;return __module.exports;
    `
    return new Function('require', wrapped)()
  } catch (e) {
    return { __error: e.message }
  }
}

// 编译 + 跑
async function run(f) {
  const content = fs.readFileSync(f, 'utf8')
  const { descriptor } = parse(content, { filename: f })
  if (!descriptor.script && !descriptor.scriptSetup) return null
  
  let scriptCode = descriptor.scriptSetup?.content || descriptor.script?.content || ''
  if (!scriptCode) return null
  
  // 编译 script
  let compiledScript
  try {
    const result = compileScript(descriptor, { id: f })
    compiledScript = result.content
  } catch (e) {
    return { error: 'compile script: ' + e.message }
  }
  
  // 编译 template
  let templateResult
  try {
    templateResult = compileTemplate({
      source: descriptor.template.content,
      filename: f,
      id: f,
      scoped: descriptor.styles.some(s => s.scoped),
    })
  } catch (e) {
    return { error: 'compile template: ' + e.message }
  }
  
  // 拼装
  const fullCode = `
    ${compiledScript}
    ${templateResult.code}
    ;return { __render, __setup: typeof setup !== 'undefined' ? setup : null }
  `
  
  let result
  try {
    const factory = new Function('Vue', 'return (() => { ' + fullCode + ' })()')
    result = factory({ h, defineComponent })
  } catch (e) {
    return { error: 'eval: ' + e.message }
  }
  
  if (!result || !result.__render) return null
  
  // 模拟 setup 调用 - 注入 mock
  const mockReturn = { __render: result.__render }
  
  // 跑 render 函数, 传入 mock vnode context
  try {
    const mockProxy = new Proxy({}, {
      get(t, k) {
        // 任何属性访问都返 mock 函数
        if (typeof k === 'string') {
          if (k.startsWith('on')) return () => {}
          if (k === 'is' || k === 'class' || k === 'style') return undefined
          // 默认 mock
          if (k === 't' || k === 'route' || k === 'router') return undefined
          return new Proxy(function(){}, {
            get(t, k2) {
              if (k2 === 'value') return undefined
              return undefined
            }
          })
        }
        return undefined
      }
    })
    
    // 直接调 render 函数
    const vnode = result.__render(mockProxy, null)
    return { ok: true }
  } catch (e) {
    return { error: 'render: ' + e.message }
  }
}

let total = 0
const reports = []
for (const f of walk('src')) {
  const r = await run(f)
  if (r && r.error) {
    if (!r.error.includes('mock') && !r.error.includes('__render')) {
      reports.push({ f, error: r.error })
      total++
    }
  }
}

for (const r of reports.slice(0, 30)) {
  console.log(`  ${r.f}: ${r.error}`)
}
console.log(`\nTotal: ${total} render errors`)
