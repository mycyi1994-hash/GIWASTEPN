#!/usr/bin/env python3
"""시그니처를 바꾼 함수의 **호출부**를 전부 찾아 인자 개수를 맞춰 본다.

컴파일러가 잡아 줄 일이지만, 이 저장소에서는 안드로이드 빌드를 로컬에서
돌릴 수 없어 CI 한 판(7분)을 태워야 알게 된다. 그 한 판을 아끼려는 검사다.

바꾼 파일만 보는 검사로는 이걸 못 잡는다. 실제로 놓쳤다 — goalBonus 에 인자를
하나 더했는데 테스트 파일의 호출부가 그대로였고, 그 파일은 내가 건드리지
않았으니 검사 대상에도 없었다.

    scripts/check-callers.py goalBonus 2

기본값이 있는 인자가 섞인 함수(컴포저블이 대개 그렇다)에는 쓰지 않는다 —
호출마다 개수가 달라도 맞는 코드라서, 여기 걸리는 것이 곧 오류가 아니다.
인자가 전부 필수인 함수에만 의미가 있다.
"""
import pathlib
import re
import sys


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2

    name = sys.argv[1]
    expected = int(sys.argv[2])
    root = pathlib.Path(__file__).resolve().parent.parent / "app" / "src"

    bad = []
    for path in root.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(rf"(?<![\w.]){re.escape(name)}\s*\(", text):
            # 선언부는 건너뛴다
            line_start = text.rfind("\n", 0, match.start()) + 1
            line = text[line_start:text.find("\n", match.start())]
            if "fun " in line:
                continue

            # 괄호 짝을 맞춰 인자 부분만 떼어 낸다
            depth, i = 0, match.end() - 1
            while i < len(text):
                if text[i] == "(":
                    depth += 1
                elif text[i] == ")":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
            args = text[match.end():i]

            # 최상위 콤마만 센다 (중첩 호출·람다 안의 콤마는 제외)
            depth, count = 0, 0
            for ch in args:
                if ch in "([{":
                    depth += 1
                elif ch in ")]}":
                    depth -= 1
                elif ch == "," and depth == 0:
                    count += 1
            # 코틀린은 마지막 인자 뒤 콤마를 허용한다. 그걸 세면 한 개가 더 있는 줄 안다.
            trimmed = args.strip()
            if trimmed.endswith(","):
                count -= 1
            actual = 0 if not trimmed else count + 1

            if actual != expected:
                lineno = text.count("\n", 0, match.start()) + 1
                bad.append(f"{path.relative_to(root.parent.parent)}:{lineno} 인자 {actual}개 (기대 {expected}개)")

    if bad:
        print(f"'{name}' 호출부가 맞지 않습니다:")
        for b in bad:
            print("  " + b)
        return 1

    print(f"'{name}' 호출부 이상 없음")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
