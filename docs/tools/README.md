# docs/tools — PDF 굽는 도구

`docs/`의 PDF 세 개는 전부 이 폴더에서 다시 만들 수 있습니다. PDF는 산출물이고,
고칠 것은 언제나 원본입니다.

| 산출물 | 원본 | 만드는 법 |
|---|---|---|
| `../StepUp-Architecture.pdf` | `../ARCHITECTURE.md` | `python3 docs/tools/build-pdf.py` |
| `../StepUp-PitchDeck.pdf` | `pitch-deck.html` | 크로미움 헤드리스 인쇄 (아래) |
| `../StepUp-PitchDeck-KO.pdf` | `pitch-deck-ko.html` | 크로미움 헤드리스 인쇄 (아래) |

## 기술 문서 PDF

```bash
python3 docs/tools/build-pdf.py              # 영문판 (기본)
python3 docs/tools/build-pdf.py --lang ko    # 한국어판
python3 docs/tools/build-pdf.py --lang both  # 합본
```

선행 조건은 `build-pdf.py` 첫머리 주석에 적혀 있습니다 — `markdown`, `playwright`,
`poppler-utils`, `fonts-noto-cjk`.

표지와 본문을 따로 인쇄해 `pdfunite`로 합칩니다. Playwright의 `margin` 옵션이
CSS `@page :first`를 덮어쓰기 때문에 한 번에 구우면 표지가 본문 첫 쪽에 겹쳐
찍힙니다. `print-pdf.mjs`가 `cover`/`body` 두 모드를 받는 이유입니다.

## 피치덱 PDF

`pitch-deck.html`·`pitch-deck-ko.html`은 15장짜리 덱의 **편집 원본**입니다.
960×540pt(16:9) 고정 페이지로 짜여 있고, 팔레트는 앱의 `ui/theme/Color.kt`를
그대로 씁니다.

```bash
chromium --headless --disable-gpu --no-pdf-header-footer \
  --print-to-pdf=docs/StepUp-PitchDeck.pdf \
  docs/tools/pitch-deck.html
```

`@media print`에서 `clamp()`를 전부 px로 못박아 두었습니다 — 페이지 크기가
고정이라 뷰포트에 따라 글자 크기가 달라지면 안 됩니다. 이 부분을 건드릴 때는
뽑은 뒤 15장을 눈으로 확인하세요.

한국어판은 Pretendard로 조판했습니다. 라틴 기준 자간·행간을 한글에 그대로 쓰면
자소가 붙으므로, 제목 자간과 행간은 한국어판에서 따로 잡혀 있습니다.
