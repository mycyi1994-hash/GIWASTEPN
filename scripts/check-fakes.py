#!/usr/bin/env python3
"""테스트의 가짜 구현이 인터페이스를 다 덮고 있는지 본다.

인터페이스에 멤버를 하나 더하면, 그 인터페이스를 구현한 **테스트의 가짜**가
전부 깨진다. 컴파일러가 잡아 주는 일이지만 여기서는 안드로이드 빌드를
로컬에서 못 돌려 CI 한 판(3분)을 태워야 알게 된다. 실제로 그렇게 태웠다 —
WalkSessionDao 에 crewDistances 를 더하고 ClaimUploadTest.FakeDao 를 그대로 뒀다.

    scripts/check-fakes.py

인터페이스 이름으로 찾으므로 같은 이름이 둘이면 둘 다 본다. 상속으로 받은
기본 구현(인터페이스의 body 있는 멤버)은 빼고 센다.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def kotlin_files(*roots):
    for r in roots:
        for base, _, files in os.walk(os.path.join(ROOT, r)):
            for f in files:
                if f.endswith(".kt"):
                    yield os.path.join(base, f)


def supertypes(header):
    """클래스 이름과 여는 중괄호 사이에서 상위 타입 이름들을 뽑는다.

    생성자 인자에도 콜론이 있으므로(`class F(a: List<X>) : Dao`) 괄호·꺾쇠
    밖에 있는 콜론만 상위 타입의 시작으로 친다.
    """
    depth = 0
    start = -1
    for i, ch in enumerate(header):
        if ch in "(<[":
            depth += 1
        elif ch in ")>]":
            depth -= 1
        elif ch == ":" and depth == 0:
            start = i + 1
            break
    if start < 0:
        return []
    names, depth, buf = [], 0, ""
    for ch in header[start:]:
        if ch in "(<[":
            depth += 1
        elif ch in ")>]":
            depth -= 1
        if ch == "," and depth == 0:
            names.append(buf)
            buf = ""
        else:
            buf += ch
    names.append(buf)
    out = []
    for n in names:
        n = n.strip()
        # `by delegate` 로 넘긴 것은 구현할 필요가 없다
        if " by " in n:
            continue
        n = n.split("(")[0].split("<")[0].strip()
        if n:
            out.append(n)
    return out


def body_of(src, start):
    """start 위치의 여는 중괄호부터 짝이 맞는 닫는 중괄호까지."""
    i = src.find("{", start)
    if i < 0:
        return ""
    depth, j = 0, i
    while j < len(src):
        if src[j] == "{":
            depth += 1
        elif src[j] == "}":
            depth -= 1
            if depth == 0:
                return src[i + 1:j]
        j += 1
    return src[i + 1:]


# 인터페이스마다 "구현해야 하는" 멤버 이름
required = {}
for path in kotlin_files("app/src/main/java"):
    src = open(path, encoding="utf-8").read()
    for m in re.finditer(r'^\s*(?:@\w+\s*\n\s*)*(?:\w+\s+)*interface\s+(\w+)', src, re.M):
        body = body_of(src, m.end())
        names = set()
        for mm in re.finditer(r'^\s*(?:override\s+)?(?:suspend\s+)?(?:fun|val|var)\s+(\w+)([^\n]*)', body, re.M):
            name, rest = mm.group(1), mm.group(2)
            # 본문이나 기본값이 있으면 구현할 필요가 없다
            if "=" in rest or rest.rstrip().endswith("{"):
                continue
            names.add(name)
        if names:
            required.setdefault(m.group(1), set()).update(names)

problems = []
for path in kotlin_files("app/src/test/java", "app/src/androidTest/java"):
    if not os.path.exists(path):
        continue
    src = open(path, encoding="utf-8").read()
    for m in re.finditer(r'^[ \t]*(?:\w+[ \t]+)*(?:class|object)[ \t]+(\w+)', src, re.M):
        brace = src.find("{", m.end())
        if brace < 0:
            continue
        header = src[m.end():brace]
        supers = supertypes(header)
        if not supers:
            continue
        body = body_of(src, brace)
        have = set(re.findall(r'\boverride\s+(?:suspend\s+)?(?:fun|val|var)\s+(\w+)', body))
        for sup in supers:
            need = required.get(sup)
            if not need:
                continue
            missing = sorted(need - have)
            if missing:
                rel = os.path.relpath(path, ROOT)
                problems.append(f"{rel}: {m.group(1)} 이 {sup} 의 {', '.join(missing)} 을 구현하지 않았다")

for p in problems:
    print(p)
print("가짜 구현 검사:", "문제 없음" if not problems else f"{len(problems)}건")
sys.exit(1 if problems else 0)
