import { defineConfig, globalIgnores } from 'eslint/config'
import globals from 'globals'
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default defineConfig([
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}'],
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**']),

  {
    languageOptions: {
      globals: {
        ...globals.browser,
      },
    },
    rules: {
      'no-unused-vars': ['error', { caughtErrors: 'none' }],
    },
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],

  {
    // componentes base no estilo shadcn: nomes de uma só palavra são intencionais
    name: 'app/ui-components',
    files: ['src/components/ui/**/*.vue'],
    rules: { 'vue/multi-word-component-names': 'off' },
  },
  {
    name: 'app/single-word-pages',
    files: ['src/App.vue', 'src/pages/**/*.vue'],
    rules: { 'vue/multi-word-component-names': 'off' },
  },
  {
    name: 'app/tests',
    files: ['**/*.test.js'],
    languageOptions: { globals: { ...globals.node } },
  },

  skipFormatting,
])
