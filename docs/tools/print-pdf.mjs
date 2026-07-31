// HTML → PDF. Playwright가 있으면 쪽번호가 박힌 PDF를, 없으면 호출한 쪽에서
// chromium CLI로 폴백한다.
// ESM import는 NODE_PATH를 보지 않으므로, 전역 설치본을 CJS require로 찾는다.
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const [, , htmlPath, pdfPath] = process.argv

const browser = await chromium.launch()
const page = await browser.newPage()
await page.goto(`file://${htmlPath}`, { waitUntil: 'networkidle' })
await page.pdf({
  path: pdfPath,
  format: 'A4',
  printBackground: true,
  displayHeaderFooter: true,
  headerTemplate: '<div></div>',
  footerTemplate: `
    <div style="width:100%;font-size:7pt;color:#8A939B;padding:0 15mm;
                font-family:-apple-system,'Segoe UI',sans-serif;
                display:flex;justify-content:space-between">
      <span>StepUp — Technical Architecture</span>
      <span class="pageNumber"></span>
    </div>`,
  margin: { top: '17mm', right: '15mm', bottom: '16mm', left: '15mm' },
})
await browser.close()
