import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './tests', testMatch: '**/*.spec.js', timeout: 45000,
  workers: 2, reporter: [['list'], ['json', { outputFile: 'artifacts/browser-results.json' }]],
  use: { baseURL: 'http://127.0.0.1:5174', screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'android-layout', use: { ...devices['Pixel 7'] } },
  ],
  webServer: [{ command: 'npm run preview', url: 'http://127.0.0.1:5174', reuseExistingServer: !process.env.CI },
    { command: 'npm run dev:pro', url: 'http://127.0.0.1:5187', reuseExistingServer: !process.env.CI, env: { VITE_PRO_PREVIEW: '1' } }],
});
