#!/usr/bin/env python3
"""
docs/ARCHITECTURE.md → docs/StepUp-Architecture.pdf

기술 문서를 지원서·투자자에게 그대로 보낼 수 있는 PDF로 굽는다.
마크다운은 GitHub에서 읽히지만, 링크 하나만 제출해야 하는 자리에서는
PDF가 더 나은 그릇이다.

    python3 docs/tools/build-pdf.py              # 영문판 (기본)
    python3 docs/tools/build-pdf.py --lang ko    # 한국어판
    python3 docs/tools/build-pdf.py --lang both  # 영문 + 한국어 합본

표지와 본문을 따로 인쇄해 합친다. 표지는 재단선 없는 전면 검정이라 여백이 0이어야
하고, 본문은 여백과 쪽번호가 있어야 한다. Playwright의 margin 옵션이 CSS
`@page :first`를 덮어쓰기 때문에 한 번에 굽는 방법으로는 둘을 동시에 만족시킬 수
없다 — 표지가 본문 첫 쪽으로 흘러넘쳐 겹쳐 찍힌다.

선행 조건
  pip3 install markdown
  npm  i -g playwright          (없으면 chromium CLI로 자동 폴백 — 쪽번호만 빠진다)
  apt  install poppler-utils    (pdfunite로 표지+본문 병합)
  apt  install fonts-noto-cjk   (없으면 한글이 두부(□)로 나온다)
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import markdown

DOCS = Path(__file__).resolve().parent.parent
TOOLS = Path(__file__).resolve().parent
SOURCE = DOCS / "ARCHITECTURE.md"
HELPER = TOOLS / "print-pdf.mjs"

# 영문 본문과 한국어 본문을 가르는 지점. 문서에 있는 앵커를 그대로 쓴다.
LANG_MARKER = '<a name="한국어"></a>'

OUTPUTS = {
    "en": DOCS / "StepUp-Architecture.pdf",
    "ko": DOCS / "StepUp-Architecture-KO.pdf",
    "both": DOCS / "StepUp-Architecture-EN-KO.pdf",
}

# 표지에 박는 값 — 문서 본문과 어긋나지 않게 여기서만 관리한다.
COVER = {
    "eyebrow": "Product architecture &amp; technical implementation",
    "title": "StepUp",
    "subtitle": "Technical Architecture",
    "tagline": "How a run on a phone becomes SUP on GIWA",
    "meta": [
        ("App", "v1.15.1 · Android · Kotlin + Jetpack Compose"),
        ("Attester", "Cloudflare Worker · viem · EIP-712"),
        ("Chain", "GIWA Sepolia · chain 91342 · 4 contracts live"),
        ("Repository", "github.com/mycyi1994-hash/GIWASTEPN"),
    ],
    "foot": "Every number in this document is a constant you can open in the repository.",
}

COVER_KO = {
    **COVER,
    "eyebrow": "제품 아키텍처 · 기술 구현",
    "subtitle": "기술 아키텍처",
    "tagline": "폰에서 뛴 러닝이 GIWA 위의 SUP가 되기까지",
    "foot": "이 문서의 모든 숫자는 저장소에서 열어 확인할 수 있는 상수입니다.",
}

FONT_STACK = (
    '"Pretendard", "Noto Sans CJK KR", "Noto Sans KR", "Noto Sans", '
    '-apple-system, "Segoe UI", sans-serif'
)
MONO_STACK = '"DejaVu Sans Mono", "Noto Sans Mono", ui-monospace, monospace'

COVER_CSS = f"""
@page {{ size: A4; margin: 0; }}
* {{ box-sizing: border-box; }}
body {{ margin: 0; font-family: {FONT_STACK}; -webkit-font-smoothing: antialiased; }}
.cover {{
  background: #07090A; color: #F2F5F7;
  width: 210mm; height: 297mm; padding: 34mm 22mm 20mm;
  display: flex; flex-direction: column; justify-content: space-between;
  overflow: hidden;
}}
.eyebrow {{
  font-size: 8.5pt; letter-spacing: 3px; text-transform: uppercase;
  color: #C3FF3E; font-weight: 700;
}}
h1 {{
  font-size: 60pt; font-weight: 900; font-style: italic; letter-spacing: -3px;
  margin: 10mm 0 0; color: #fff;
}}
h1 .dot {{ color: #C3FF3E; }}
.subtitle {{ font-size: 17pt; font-weight: 700; margin-top: 5mm; color: #fff; }}
.tagline {{ font-size: 11pt; color: #9FAAB3; margin-top: 3mm; }}
.rule {{ height: 3px; background: #C3FF3E; width: 46mm; margin: 9mm 0; }}
table {{ width: 100%; border-collapse: collapse; font-size: 9.5pt; }}
td {{ padding: 2.2mm 0; vertical-align: top; }}
td:first-child {{ color: #C3FF3E; font-weight: 700; width: 32mm; letter-spacing: .4px; }}
td:last-child {{ color: #C9D2D8; }}
.foot {{ font-size: 8.5pt; color: #6B7480; line-height: 1.7; }}
"""

BODY_CSS = f"""
:root {{
  --ink:#0C0E10; --body:#232A30; --muted:#5C666F; --line:#D8DEE3;
  --volt:#5B8C00; --volt-bg:#F3FAE2;
}}

* {{ box-sizing: border-box; }}

body {{
  font-family: {FONT_STACK};
  font-size: 10pt; line-height: 1.62; color: var(--body);
  margin: 0; -webkit-font-smoothing: antialiased;
  word-break: keep-all; /* 한글이 단어 중간에서 끊기지 않게 */
}}

h1 {{
  font-size: 21pt; font-weight: 900; color: var(--ink); letter-spacing: -.6px;
  margin: 0 0 6mm; padding-bottom: 3mm; border-bottom: 2.5px solid var(--ink);
  page-break-before: always; page-break-after: avoid;
}}
/* 언어 합본에서만 h1이 둘 이상이다. 첫 h1은 이미 새 쪽이라 강제 개행이 필요 없다 */
body > main > h1:first-child {{ page-break-before: avoid; }}
h2 {{
  font-size: 14pt; font-weight: 800; color: var(--ink); letter-spacing: -.3px;
  margin: 9mm 0 3.5mm; padding-left: 4mm; border-left: 4px solid var(--volt);
  page-break-after: avoid;
}}
h3 {{
  font-size: 11.5pt; font-weight: 700; color: var(--ink);
  margin: 6mm 0 2.5mm; page-break-after: avoid;
}}
p {{ margin: 0 0 3.2mm; }}
strong {{ color: var(--ink); font-weight: 700; }}
a {{ color: var(--volt); text-decoration: none; }}
ul, ol {{ margin: 0 0 3.2mm; padding-left: 6mm; }}
li {{ margin-bottom: 1.4mm; }}
hr {{ border: 0; border-top: 1px solid var(--line); margin: 7mm 0; }}

table {{
  width: 100%; max-width: 100%; border-collapse: collapse;
  margin: 3mm 0 5mm; font-size: 8.8pt;
  /* table-layout은 auto — 열 너비를 내용에 맞춘다. 인쇄 영역을 넘지 않는 것은
     아래 overflow-wrap이 보장한다(긴 경로·식별자도 셀 안에서 접힌다). */
}}
th, td {{
  border: 1px solid var(--line); padding: 2mm 2.6mm; text-align: left;
  vertical-align: top;
  /* 셀 안의 긴 경로·식별자가 표를 인쇄 영역 밖으로 밀어내지 않게 */
  overflow-wrap: anywhere; word-break: break-word;
}}
th {{ background: var(--volt-bg); color: var(--ink); font-weight: 700; }}
tr {{ page-break-inside: avoid; }}
td code, th code {{ font-size: 8pt; }}

code {{
  font-family: {MONO_STACK};
  font-size: 8.6pt; background: #F1F4F6; padding: .4mm 1.1mm; border-radius: 2px;
  color: #14181B; overflow-wrap: anywhere;
}}
pre {{
  background: #F7F9FA; border: 1px solid var(--line); border-left: 3px solid var(--volt);
  padding: 3.5mm 4mm; margin: 3mm 0 5mm; overflow: visible;
  page-break-inside: avoid; border-radius: 3px;
}}
pre code {{
  background: none; padding: 0; font-size: 6.9pt; line-height: 1.42;
  white-space: pre; display: block; overflow-wrap: normal;
}}

blockquote {{
  margin: 3mm 0 4mm; padding: 2.5mm 4mm; border-left: 3px solid var(--line);
  color: var(--muted); background: #FAFBFC;
}}

/* 가운데 정렬 블록(문서 머리·꼬리)과 그 뒤에 붙는 구분선은 PDF에선 군더더기 */
div[align="center"] {{ display: none; }}
div[align="center"] + hr {{ display: none; }}

/* 헤더 없이 쓰는 2단 표는 빈 헤더 줄이 남는다 — 쪽이 넘어갈 때 특히 눈에 띈다 */
thead:not(:has(th:not(:empty))) {{ display: none; }}

/* "이 문서의 PDF" 링크는 PDF 안에서 자기 자신을 가리키게 되므로 뺀다 */
p:has(a[href$=".pdf"]) {{ display: none; }}

img {{ max-width: 100%; }}
"""

HELPER_JS = """\
// HTML → PDF. 인자: <html> <pdf> <mode: cover|body>
// ESM import는 NODE_PATH를 보지 않으므로, 전역 설치본을 CJS require로 찾는다.
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const [, , htmlPath, pdfPath, mode] = process.argv

const browser = await chromium.launch()
const page = await browser.newPage()
await page.goto(`file://${htmlPath}`, { waitUntil: 'networkidle' })

const options =
  mode === 'cover'
    ? { printBackground: true, margin: { top: 0, right: 0, bottom: 0, left: 0 } }
    : {
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
      }

await page.pdf({ path: pdfPath, format: 'A4', ...options })
await browser.close()
"""


def split_source(lang: str) -> str:
    """영문 본문 / 한국어 본문을 갈라낸다."""
    text = SOURCE.read_text(encoding="utf-8")
    if lang == "both":
        return text
    if LANG_MARKER not in text:
        return text
    english, korean = text.split(LANG_MARKER, 1)
    if lang == "ko":
        return korean
    # 영문 끝에 남는 구분선(--- / ---)은 본문 마지막에 빈 줄만 남기므로 걷어낸다
    return english.rstrip().rstrip("-").rstrip()


def cover_html(lang: str) -> str:
    cover = COVER_KO if lang == "ko" else COVER
    rows = "".join(f"<tr><td>{k}</td><td>{v}</td></tr>" for k, v in cover["meta"])
    subtitle = cover["subtitle"]
    if lang == "both":
        subtitle = f"{COVER['subtitle']} · {COVER_KO['subtitle']}"
    return f"""<!doctype html>
<html lang="{lang}"><head><meta charset="utf-8">
<title>StepUp — Technical Architecture</title>
<style>{COVER_CSS}</style></head>
<body><section class="cover">
  <div>
    <div class="eyebrow">{cover['eyebrow']}</div>
    <h1>{cover['title']}<span class="dot">.</span></h1>
    <div class="subtitle">{subtitle}</div>
    <div class="tagline">{cover['tagline']}</div>
  </div>
  <div>
    <div class="rule"></div>
    <table>{rows}</table>
  </div>
  <div class="foot">{cover['foot']}</div>
</section></body></html>
"""


def body_html(lang: str) -> str:
    body = markdown.markdown(
        split_source(lang),
        extensions=["tables", "fenced_code", "attr_list", "md_in_html", "toc"],
    )
    return f"""<!doctype html>
<html lang="{lang}"><head><meta charset="utf-8">
<title>StepUp — Technical Architecture</title>
<style>{BODY_CSS}</style></head>
<body><main>{body}</main></body></html>
"""


def write_temp(suffix: str, content: str) -> Path:
    with tempfile.NamedTemporaryFile("w", suffix=suffix, delete=False, encoding="utf-8") as f:
        f.write(content)
        return Path(f.name)


def find_chromium() -> str | None:
    return (
        shutil.which("chromium")
        or shutil.which("chromium-browser")
        or shutil.which("google-chrome")
        or next(
            (str(p) for p in sorted(Path("/opt/pw-browsers").glob("chromium-*/chrome-linux/chrome"))),
            None,
        )
    )


def print_pdf(html: Path, pdf: Path, mode: str) -> None:
    HELPER.write_text(HELPER_JS, encoding="utf-8")
    env = {**os.environ, "NODE_PATH": os.environ.get("NODE_PATH", "/opt/node22/lib/node_modules")}
    try:
        subprocess.run(
            ["node", str(HELPER), str(html), str(pdf), mode],
            check=True, env=env, capture_output=True, text=True,
        )
        return
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        detail = getattr(e, "stderr", "") or str(e)
        print(f"playwright 실패 → chromium CLI로 폴백합니다\n{detail[:400]}", file=sys.stderr)

    chrome = find_chromium()
    if not chrome:
        sys.exit("chromium을 찾지 못했습니다.")
    subprocess.run(
        [chrome, "--headless", "--disable-gpu", "--no-sandbox",
         f"--print-to-pdf={pdf}", "--no-pdf-header-footer", f"file://{html}"],
        check=True, capture_output=True,
    )


def build(lang: str) -> Path:
    out = OUTPUTS[lang]
    cover_pdf = Path(tempfile.mkdtemp()) / "cover.pdf"
    body_pdf = cover_pdf.with_name("body.pdf")

    cover_file = write_temp("-cover.html", cover_html(lang))
    body_file = write_temp("-body.html", body_html(lang))
    try:
        print_pdf(cover_file, cover_pdf, "cover")
        print_pdf(body_file, body_pdf, "body")
    finally:
        cover_file.unlink(missing_ok=True)
        body_file.unlink(missing_ok=True)

    if shutil.which("pdfunite"):
        subprocess.run(["pdfunite", str(cover_pdf), str(body_pdf), str(out)], check=True)
    else:
        print("pdfunite(poppler-utils)가 없어 표지를 빼고 본문만 씁니다.", file=sys.stderr)
        shutil.copy(body_pdf, out)
    shutil.rmtree(cover_pdf.parent, ignore_errors=True)
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description="ARCHITECTURE.md → PDF")
    parser.add_argument("--lang", choices=("en", "ko", "both"), default="en")
    args = parser.parse_args()

    if not SOURCE.exists():
        sys.exit(f"{SOURCE} 가 없습니다.")

    out = build(args.lang)
    pages = ""
    if shutil.which("pdfinfo"):
        info = subprocess.run(["pdfinfo", str(out)], capture_output=True, text=True).stdout
        pages = next(
            (f" · {line.split(':')[1].strip()}쪽" for line in info.splitlines() if line.startswith("Pages:")),
            "",
        )
    print(f"✓ {out.relative_to(DOCS.parent)} ({out.stat().st_size / 1024:.0f} KB{pages})")


if __name__ == "__main__":
    main()
