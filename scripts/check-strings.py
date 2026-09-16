#!/usr/bin/env python3
"""문자열 리소스가 안드로이드 빌드를 깨뜨리지 않는지 확인한다.

안드로이드는 이스케이프하지 않은 아포스트로피(')를 거부한다. 오류 메시지가
"Invalid unicode escape sequence" 라고 나와서 원인을 알아보기 어렵고, 빌드
7분을 쓴 뒤에야 알게 된다. 여기서 먼저 잡는다.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"

STRING = re.compile(r'<string name="([^"]+)"[^>]*>(.*?)</string>', re.S)


def problems(path: pathlib.Path):
    text = path.read_text(encoding="utf-8")
    for match in STRING.finditer(text):
        name, body = match.group(1), match.group(2)
        line = text[: match.start()].count("\n") + 1

        # 통째로 따옴표로 감싼 경우는 안드로이드가 그대로 받는다
        quoted = body.startswith('"') and body.endswith('"')
        if not quoted and re.search(r"(?<!\\)'", body):
            yield line, name, "이스케이프하지 않은 아포스트로피 — \\' 로 쓰세요", body
        if re.search(r"(?<!\\)&(?!amp;|lt;|gt;|quot;|apos;|#)", body):
            yield line, name, "이스케이프하지 않은 & — &amp; 로 쓰세요", body


def main() -> int:
    found = 0
    for path in sorted(RES.glob("values*/strings.xml")):
        for line, name, why, body in problems(path):
            rel = path.relative_to(ROOT)
            print(f"{rel}:{line}  {name}\n    {why}\n    {body.strip()[:80]}", file=sys.stderr)
            found += 1
    if found:
        print(f"\n문자열 리소스 문제 {found}건", file=sys.stderr)
        return 1
    print("문자열 리소스 이상 없음")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
