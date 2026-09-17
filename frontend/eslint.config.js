// ESLint flat config for the FORMA frontend (FOR-85).
// Baseline linting for React + TypeScript; Prettier owns formatting, so
// eslint-config-prettier disables any stylistic rules that would conflict.
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import prettier from 'eslint-config-prettier';

export default tseslint.config(
  // `nginx/test/echo-backend.mjs` is Node-only Docker test tooling (double-proxy nginx
  // coverage, FOR- bug fix), not SPA source — it runs under `node:22-alpine` in CI, never
  // bundled, and uses Node globals (`console`) the browser lint config below doesn't know.
  { ignores: ['dist', 'node_modules', 'coverage', 'nginx'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
  prettier,
);
