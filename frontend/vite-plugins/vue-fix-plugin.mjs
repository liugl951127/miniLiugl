function fixVueCode(code) {
  let fixed = code
  
  // 1. 修 <div class="xxx yyy> + <div class="zzz"> 形式
  fixed = fixed.replace(
    /<div class="([^"\s>]+(?:\s+[^"\s>]+)*)>\s*\n\s*<div class="([^"]+)">/g,
    '<div class="$1 $2">'
  )
  fixed = fixed.replace(
    /<div class="([^"\s>]+)>\s*\n\s*<div class="([^"]+)">/g,
    '<div class="$1 $2">'
  )
  
  // 2. + :class
  fixed = fixed.replace(
    /<div class="([^"\s>]+(?:\s+[^"\s>]+)*)>\s*\n\s*<div class="([^"]+)">\s*(:class="[^"]+")\s*>/g,
    '<div class="$1 $2" $3>'
  )
  fixed = fixed.replace(
    /<div class="([^"\s>]+)>\s*\n\s*<div class="([^"]+)">\s*(:class="[^"]+")\s*>/g,
    '<div class="$1 $2" $3>'
  )
  fixed = fixed.replace(
    /">\s+(:class="[^"]+")\s*>/g,
    '" $1>'
  )
  
  // 3. v-bind:title
  fixed = fixed.replace(
    /v-bind:title="\\'([\s\S]*?)\\'"/g,
    ':title="`$1`"'
  )
  
  // 4. 末尾多余 </div>
  let prev = ''
  let safety = 0
  while (prev !== fixed && safety < 10) {
    prev = fixed
    fixed = fixed.replace(/(\s*)<\/div>\s*<\/div>/g, '$1</div>')
    fixed = fixed.replace(/(\s*)<\/div>\s*\n\s*<\/div>\s*\n\s*<\/template>/, '$1</div>\n</template>')
    safety++
  }
  fixed = fixed.replace(/(\/>)\s*<\/div>\s*<\/template>/, '$1\n</template>')
  
  // 5. 末尾加缺
  const templateMatch = fixed.match(/<template>([\s\S]*?)<\/template>/)
  if (templateMatch) {
    const tmpl = templateMatch[1]
    const opens = (tmpl.match(/<div[\s>]/g) || []).length
    const closes = (tmpl.match(/<\/div>/g) || []).length
    const diff = opens - closes
    if (diff > 0) {
      const closeStr = '\n  </div>'.repeat(diff) + '\n</template>'
      fixed = fixed.replace(/\n<\/template>/, closeStr)
    } else if (diff < 0) {
      const excess = -diff
      for (let i = 0; i < excess; i++) {
        fixed = fixed.replace(/(\s*)<\/div>(\s*)<\/template>/, '$1$2</template>')
      }
    }
  }
  
  return fixed
}

export const vueFixPlugin = {
  name: 'vue-template-fix',
  enforce: 'pre',
  transform(code, id) {
    if (id.endsWith('.vue') && id.includes('src/')) {
      const fixed = fixVueCode(code)
      if (fixed !== code) {
        return { code: fixed, map: null }
      }
    }
  }
}
