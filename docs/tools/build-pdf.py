#!/usr/bin/env python3
"""
docs/ARCHITECTURE.md → docs/StepUp-Architecture.pdf

기술 문서를 지원서·투자자에게 그대로 보낼 수 있는 PDF로 굽는다.
마크다운은 GitHub에서 읽히지만, 링크 하나만 제출해야 하는 자리에서는
PDF가 더 나은 그릇이다.

    python3 docs/tools/build-pdf.py

선행 조건
  pip3 install markdown
  npm  i -g playwright          (없으면 chromium CLI로 자동 폴백 — 쪽번호만 빠진다)
  apt  install fonts-noto-cjk   (없으면 한글이 두부(□)로 나온다)
"""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import markdown

DOCS = Path(__file__).resolve().parent.parent
SOURCE = DOCS / "ARCHITECTURE.md"
OUTPUT = DOCS / "StepUp-Architecture.pdf"
HELPER = Path(__file__).resolve().parent / "print-pdf.mjs"

# 표지에 박는 값 — 문서 본문과 어긋나지 않게 여기서만 관리한다.
COVER = {
    "title": "StepUp",
    "subtitle": "Technical Architecture · 기술 아키텍처",
    "tagline": "How a run on a phone becomes SUP on GIWA",
    "meta": [
        ("App", "v1.15.1 · Android · Kotlin + Jetpack Compose"),
        ("Attester", "Cloudflare Worker · viem · EIP-712"),
        ("Chain", "GIWA Sepolia · chain 91342 · 4 contracts live"),
        ("Repository", "github.com/mycyi1994-hash/GIWASTEPN"),
    ],
}

CSS = """
@page { size: A4; margin: 17mm 15mm 16mm; }
@page :first { margin: 0; }

:root {
  --ink:#0C0E10; --body:#232A30; --muted:#5C666F; --line:#D8DEE3;
  --volt:#5B8C00; --volt-bg:#F3FAE2; --black:#07090A; --neon:#C3FF3E;
}

* { box-sizing: border-box; }

body {
  font-family: "Pretendard", "Noto Sans CJK KR", "Noto Sans KR", "Noto Sans",
               -apple-system, "Segoe UI", sans-serif;
  font-size: 10pt; line-height: 1.62; color: var(--body);
  margin: 0; -webkit-font-smoothing: antialiased;
  word-break: keep-all; /* 한글이 단어 중간에서 끊기지 않게 */
}

/* ── 표지 ───────────────────────────────────────────── */
.cover {
  background: var(--black); color: #F2F5F7;
  height: 297mm; padding: 34mm 22mm 22mm;
  display: flex; flex-direction: column; justify-content: space-between;
  page-break-after: always;
}
.cover .eyebrow {
  font-size: 8.5pt; letter-spacing: 3px; text-transform: uppercase;
  color: var(--neon); font-weight: 700;
}
.cover h1 {
  font-size: 62pt; font-weight: 900; font-style: italic; letter-spacing: -3px;
  margin: 10mm 0 0; color: #fff; border: 0; padding: 0;
}
.cover h1 .dot { color: var(--neon); }
.cover .subtitle { font-size: 17pt; font-weight: 700; margin-top: 5mm; color: #fff; }
.cover .tagline { font-size: 11pt; color: #9FAAB3; margin-top: 3mm; }
.cover .rule { height: 3px; background: var(--neon); width: 46mm; margin: 9mm 0; }
.cover table { width: 100%; border: 0; font-size: 9.5pt; }
.cover td { border: 0; padding: 2.2mm 0; vertical-align: top; }
.cover td:first-child {
  color: var(--neon); font-weight: 700; width: 30mm; letter-spacing: .4px;
}
.cover td:last-child { color: #C9D2D8; }
.cover .foot { font-size: 8.5pt; color: #6B7480; }

/* ── 본문 ───────────────────────────────────────────── */
.page { padding: 0; }

h1 {
  font-size: 21pt; font-weight: 900; color: var(--ink); letter-spacing: -.6px;
  margin: 0 0 6mm; padding-bottom: 3mm; border-bottom: 2.5px solid var(--ink);
  page-break-before: always; page-break-after: avoid;
}
/* 마크다운 h1은 언어 구분(영문 본문 → 한국어 본문)에만 쓰이므로 쪽을 넘긴다.
   영문 표제는 표지가 대신하니 예외를 둘 h1이 본문에 없다. */
h2 {
  font-size: 14pt; font-weight: 800; color: var(--ink); letter-spacing: -.3px;
  margin: 9mm 0 3.5mm; padding-left: 4mm; border-left: 4px solid var(--volt);
  page-break-after: avoid;
}
h3 {
  font-size: 11.5pt; font-weight: 700; color: var(--ink);
  margin: 6mm 0 2.5mm; page-break-after: avoid;
}
p { margin: 0 0 3.2mm; }
strong { color: var(--ink); font-weight: 700; }
a { color: var(--volt); text-decoration: none; }
ul, ol { margin: 0 0 3.2mm; padding-left: 6mm; }
li { margin-bottom: 1.4mm; }
hr { border: 0; border-top: 1px solid var(--line); margin: 7mm 0; }

table {
  width: 100%; border-collapse: collapse; margin: 3mm 0 5mm; font-size: 8.8pt;
  page-break-inside: auto;
}
th, td {
  border: 1px solid var(--line); padding: 2mm 2.6mm; text-align: left;
  vertical-align: top;
}
th { background: var(--volt-bg); color: var(--ink); font-weight: 700; }
tr { page-break-inside: avoid; }
td code, th code { font-size: 8pt; }

code {
  font-family: "DejaVu Sans Mono", "Noto Sans Mono", ui-monospace, monospace;
  font-size: 8.6pt; background: #F1F4F6; padding: .4mm 1.1mm; border-radius: 2px;
  color: #14181B;
}
pre {
  background: #F7F9FA; border: 1px solid var(--line); border-left: 3px solid var(--volt);
  padding: 3.5mm 4mm; margin: 3mm 0 5mm; overflow: visible;
  page-break-inside: avoid; border-radius: 3px;
}
pre code {
  background: none; padding: 0; font-size: 6.9pt; line-height: 1.42;
  white-space: pre; display: block;
}

blockquote {
  margin: 3mm 0 4mm; padding: 2.5mm 4mm; border-left: 3px solid var(--line);
  color: var(--muted); background: #FAFBFC;
}

/* 가운데 정렬 블록(문서 머리·꼬리)과 그 뒤에 붙는 구분선은 PDF에선 군더더기 */
div[align="center"] { display: none; }
div[align="center"] + hr { display: none; }

/* 헤더 없이 쓰는 2단 표는 빈 헤더 줄이 남는다 — 쪽이 넘어갈 때 특히 눈에 띈다 */
thead:not(:has(th:not(:empty))) { display: none; }

/* "이 문서의 PDF" 링크는 PDF 안에서 자기 자신을 가리키게 되므로 뺀다 */
p:has(a[href$="StepUp-Architecture.pdf"]) { display: none; }

img { max-width: 100%; }
"""

HELPER_JS = """\
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
"""


def cover_html() -> str:
    rows = "".join(
        f"<tr><td>{label}</td><td>{value}</td></tr>" for label, value in COVER["meta"]
    )
    return f"""
<section class="cover">
  <div>
    <div class="eyebrow">Product architecture &amp; technical implementation</div>
    <h1>{COVER['title']}<span class="dot">.</span></h1>
    <div class="subtitle">{COVER['subtitle']}</div>
    <div class="tagline">{COVER['tagline']}</div>
  </div>
  <div>
    <div class="rule"></div>
    <table>{rows}</table>
  </div>
  <div class="foot">
    Every number in this document is a constant you can open in the repository.<br>
    이 문서의 모든 숫자는 저장소에서 열어 확인할 수 있는 상수입니다.
  </div>
</section>
"""


def render_html() -> str:
    body = markdown.markdown(
        SOURCE.read_text(encoding="utf-8"),
        extensions=["tables", "fenced_code", "attr_list", "md_in_html", "toc"],
    )
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<title>StepUp — Technical Architecture</title>
<style>{CSS}</style></head>
<body>{cover_html()}<main class="page">{body}</main></body></html>
"""


def print_pdf(html_path: Path) -> None:
    HELPER.write_text(HELPER_JS, encoding="utf-8")
    node_path = os.environ.get("NODE_PATH", "/opt/node22/lib/node_modules")
    env = {**os.environ, "NODE_PATH": node_path}
    try:
        subprocess.run(
            ["node", str(HELPER), str(html_path), str(OUTPUT)],
            check=True, env=env, capture_output=True, text=True,
        )
        return
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        detail = getattr(e, "stderr", "") or str(e)
        print(f"playwright 실패 → chromium CLI로 폴백합니다\n{detail[:400]}", file=sys.stderr)

    chrome = (
        shutil.which("chromium")
        or shutil.which("chromium-browser")
        or shutil.which("google-chrome")
        or next(
            (str(p) for p in sorted(Path("/opt/pw-browsers").glob("chromium-*/chrome-linux/chrome"))),
            None,
        )
    )
    if not chrome:
        sys.exit("chromium을 찾지 못했습니다.")
    subprocess.run(
        [chrome, "--headless", "--disable-gpu", "--no-sandbox",
         f"--print-to-pdf={OUTPUT}", "--no-pdf-header-footer", f"file://{html_path}"],
        check=True, capture_output=True,
    )


def main() -> None:
    if not SOURCE.exists():
        sys.exit(f"{SOURCE} 가 없습니다.")
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8") as f:
        f.write(render_html())
        html_path = Path(f.name)
    try:
        print_pdf(html_path)
    finally:
        html_path.unlink(missing_ok=True)
    size = OUTPUT.stat().st_size / 1024
    print(f"✓ {OUTPUT.relative_to(DOCS.parent)} ({size:.0f} KB)")


if __name__ == "__main__":
    main()
