/**
 * Vite plugin to stub .css imports
 */
export default function cssStubPlugin() {
  return {
    name: 'css-stub',
    enforce: 'pre',
    resolveId(id) {
      if (id.endsWith('.css') || id.includes('.css?')) {
        return { id: '\0css-stub', external: false }
      }
      return null
    },
    load(id) {
      if (id === '\0css-stub') {
        return 'export default {};'
      }
      return null
    }
  }
}
