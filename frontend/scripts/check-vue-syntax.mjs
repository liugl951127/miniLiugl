// Verify our 5 target Vue files parse correctly
import { parse } from 'vue/compiler-sfc'
import { readFileSync } from 'node:fs'

const files = [
  'src/views/model/Index.vue',
  'src/views/training/Console.vue',
  'src/views/training/Dashboard.vue',
  'src/views/multimodal/Index.vue',
  'src/views/prompts/Index.vue',
]

let allOk = true
for (const f of files) {
  const content = readFileSync(f, 'utf-8')
  const { descriptor, errors } = parse(content)
  if (errors && errors.length) {
    console.error(`ERROR in ${f}:`)
    errors.forEach(e => console.error('  ', e.message))
    allOk = false
  } else {
    console.log(`OK ${f}: template=${!!descriptor.template} script=${!!descriptor.scriptSetup} styles=${descriptor.styles.length}`)
  }
}
process.exit(allOk ? 0 : 1)
