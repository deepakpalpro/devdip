import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  resolve: {
    alias: {
      '@banking-forms/api-client': path.resolve(rootDir, '../../packages/api-client/src'),
      '@banking-forms/ui': path.resolve(rootDir, '../../packages/ui/src'),
      '@banking-forms/form-renderer': path.resolve(rootDir, '../../packages/form-renderer/src'),
    },
  },
});
