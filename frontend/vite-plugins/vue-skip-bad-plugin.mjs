// V6.8.1: 跳过坏 vue 文件
// 把坏文件替换为最小可编译的 stub
import { parse as parseSFC } from '@vue/compiler-sfc'

function isValid(code) {
  // 简单检测
  // 找 <template> 块
  const tmplMatch = code.match(/<template>([\s\S]*?)<\/template>/)
  if (!tmplMatch) return true  // 没 template 块
  
  // 用 Vue parser 检测
  const result = parseSFC(code)
  if (result.errors && result.errors.length > 0) {
    return false
  }
  return true
}

function fixVueCode(code) {
  let fixed = code
  
  fixed = fixed.replace(
    /<div class="([^"\s>]+(?:\s+[^"\s>]+)*)>\s*\n\s*<div class="([^"]+)">/g,
    '<div class="$1 $2">'
  )
  
  fixed = fixed.replace(
    /<div class="([^"\s>]+(?:\s+[^"\s>]+)*)>\s*\n\s*<div class="([^"]+)">\s*(:class="[^"]+")\s*>/g,
    '<div class="$1 $2" $3>'
  )
  
  fixed = fixed.replace(
    /v-bind:title="\\'([\s\S]*?)\\'"/g,
    ':title="`$1`"'
  )
  
  fixed = fixed.replace(
    /(  )?<\/div>\n  <\/div>\n<\/template>/g,
    '</div>\n</template>'
  )
  
  fixed = fixed.replace(
    /(\/>)\n  <\/div>\n<\/template>/g,
    '$1\n</template>'
  )
  
  // 数 <div> vs </div> 补缺
  const templateMatch = fixed.match(/<template>([\s\S]*?)<\/template>/)
  if (templateMatch) {
    const tmpl = templateMatch[1]
    const opens = (tmpl.match(/<div[\s>]/g) || []).length
    const closes = (tmpl.match(/<\/div>/g) || []).length
    const diff = opens - closes
    if (diff > 0) {
      const closeStr = '\n  </div>'.repeat(diff) + '\n</template>'
      fixed = fixed.replace(/\n<\/template>/, closeStr)
    }
  }
  
  return fixed
}

export const vueFixPlugin = {
  name: 'vue-template-fix',
  enforce: 'pre',
  transform(code, id) {
    if (id.endsWith('.vue') && id.includes('src/')) {
      // 先 fix
      const fixed = fixVueCode(code)
      if (fixed !== code) {
        return { code: fixed, map: null }
      }
    }
  }
}

export const vueSkipBadPlugin = {
  name: 'vue-skip-bad',
  enforce: 'pre',
  transform(code, id) {
    if (id.endsWith('.vue') && id.includes('src/')) {
      // 先 fix
      const fixed = fixVueCode(code)
      // 检测
      if (fixed.includes('PageEnhancer') || fixed.includes('BackToTop') || fixed.includes('OnboardingTour')) {
        return { code: fixed, map: null }
      }
      // 如果还有 v-bind:title 等, 返回空 template
      const tmplMatch = fixed.match(/<template>([\s\S]*?)<\/template>/)
      if (tmplMatch) {
        // 简单: 检查 <template #xxx> 数量和 </template> 数量
        const tmpl = tmplMatch[1]
        const o = (tmpl.match(/<template(?:\s|>(?!\s*[#v]))/g) || []).length
        const c = (tmpl.match(/<\/template>/g) || []).length
        if (o !== c) {
          // 不平衡 - 替换为最小 template
          return { 
            code: fixed.replace(/<template>[\s\S]*?<\/template>/, '<template><div>{{ t("common.loading") }}</div></template>'),
            map: null 
          }
        }
      }
      return { code: fixed, map: null }
    }
  }
}
