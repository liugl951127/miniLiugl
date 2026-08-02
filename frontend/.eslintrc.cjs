module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true
  },
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
    requireConfigFile: false
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended'
  ],
  plugins: ['@typescript-eslint'],
  globals: {
    defineProps: 'readonly',
    defineEmits: 'readonly',
    defineExpose: 'readonly',
    withDefaults: 'readonly',
    ref: 'readonly',
    reactive: 'readonly',
    computed: 'readonly',
    watch: 'readonly',
    watchEffect: 'readonly',
    onMounted: 'readonly',
    onUnmounted: 'readonly',
    onBeforeMount: 'readonly',
    onBeforeUnmount: 'readonly',
    onActivated: 'readonly',
    onDeactivated: 'readonly',
    onUpdated: 'readonly',
    onBeforeUpdate: 'readonly',
    nextTick: 'readonly',
    inject: 'readonly',
    provide: 'readonly',
    useRoute: 'readonly',
    useRouter: 'readonly',
    useStore: 'readonly'
  },
  rules: {
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    'no-undef': 'off',
    'no-empty': 'off',
    'no-useless-escape': 'off',
    'no-constant-condition': 'off',  // while(true) 模式
    'no-case-declarations': 'off',   // switch case 里 let
    'vue/multi-word-component-names': 'off',
    'vue/no-unused-vars': 'warn',
    'vue/require-default-prop': 'off',
    'vue/html-self-closing': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/max-attributes-per-line': 'off',
    'vue/html-indent': 'off',
    'vue/attributes-order': 'off',
    'vue/html-closing-bracket-newline': 'off',
    'vue/first-attribute-linebreak': 'off',
    'vue/no-v-html': 'off',
    'vue/no-mutating-props': 'warn',
    'vue/component-definition-name-casing': 'off',
    'vue/html-closing-bracket-spacing': 'off',
    'vue/attribute-hyphenation': 'off',
    'vue/v-on-event-hyphenation': 'off',
    'vue/no-parsing-error': 'off',
    'vue/no-template-shadow': 'off',
    'vue/no-unused-components': 'warn',
    'vue/custom-event-name-casing': 'off',
    'vue/return-in-computed-property': 'off',
    'vue/require-render-return': 'off',
    'vue/no-side-effects-in-computed-properties': 'warn',
    'vue/no-deprecated-v-on-native-modifier': 'off',
    'vue/no-use-v-if-with-v-for': 'off',
    'vue/no-ref-as-operand': 'off',
    'vue/multiline-html-element-content-newline': 'off'
  },
  overrides: [
    {
      // V3.7.38+ i18n 嵌套对象设计允许重复 key (chat/monitor/kg/agent/admin 在不同 module)
      files: ['src/i18n/locales/*.js'],
      rules: {
        'no-dupe-keys': 'off'
      }
    }
  ]
}
