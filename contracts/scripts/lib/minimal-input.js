/**
 * 컨트랙트 하나를 검증하는 데 **실제로 필요한 소스만** 골라낸다.
 *
 * ## 왜 필요한가
 *
 * Hardhat의 build-info에는 그 컴파일 작업에 들어간 소스가 전부 들어 있다.
 * 우리 프로젝트는 43개, 합쳐서 274KB다. 그런데 `SUPToken` 하나를 검증하는 데
 * `SneakerNFT`나 ERC-721 구현체는 필요 없다.
 *
 * 통째로 보내면 Blockscout이 크기 때문에 거절하고 JSON 대신 HTML 오류
 * 페이지를 돌려준다. hardhat-verify는 그걸 파싱하다 터진다:
 *
 *     Unexpected token '<', "<!DOCTYPE "... is not valid JSON
 *
 * 작은 컨트랙트(`CourseRegistry`)만 통과하고 OpenZeppelin을 끌어오는 셋이
 * 전부 막힌 이유가 이것이다.
 *
 * ## 어떻게 고르나
 *
 * import 문을 문자열로 파싱하지 않는다. solc가 이미 만들어 둔 AST를 쓴다.
 * AST의 `ImportDirective` 노드에는 solc가 remapping·상대경로를 전부 풀어낸
 * `absolutePath`가 들어 있다. 그걸 따라 너비 우선으로 훑으면 정확한
 * 의존성 폐포가 나온다. 추측이 끼어들 자리가 없다.
 */

/** 이 소스가 직접 import 하는 경로들 (solc가 이미 해석해 둔 것) */
function directImports(outputSources, sourceName) {
  const ast = outputSources?.[sourceName]?.ast;
  if (!ast?.nodes) return [];
  return ast.nodes
    .filter((n) => n.nodeType === "ImportDirective" && n.absolutePath)
    .map((n) => n.absolutePath);
}

/**
 * `entry` 를 컴파일하는 데 필요한 소스 이름 전부 (자기 자신 포함).
 *
 * AST가 없으면(구버전 build-info 등) `null` 을 돌려준다. 호출하는 쪽이
 * 전체 입력으로 물러설 수 있게 — **줄이려다 빠뜨리는 것보다 큰 게 낫다.**
 */
function closureOf(buildInfo, entry) {
  const outputSources = buildInfo?.output?.sources;
  if (!outputSources || !outputSources[entry]?.ast) return null;

  const seen = new Set([entry]);
  const queue = [entry];

  while (queue.length) {
    const current = queue.shift();
    for (const next of directImports(outputSources, current)) {
      if (seen.has(next)) continue;
      if (!buildInfo.input.sources[next]) continue; // 입력에 없으면 넣을 수도 없다
      seen.add(next);
      queue.push(next);
    }
  }
  return seen;
}

/**
 * `entry` 에 필요한 소스만 남긴 Standard JSON Input.
 *
 * 나머지 설정(optimizer, evmVersion, outputSelection)은 원본 그대로 둔다.
 * **바이트코드가 달라지면 검증 자체가 실패**하므로 컴파일 설정은 손대지 않는다.
 */
function minimalInput(buildInfo, entry) {
  const full = buildInfo.input;
  const keep = closureOf(buildInfo, entry);
  if (!keep) return { input: full, kept: Object.keys(full.sources).length, total: Object.keys(full.sources).length };

  const sources = {};
  for (const name of Object.keys(full.sources)) {
    if (keep.has(name)) sources[name] = full.sources[name];
  }

  return {
    input: { ...full, sources },
    kept: keep.size,
    total: Object.keys(full.sources).length,
  };
}

/**
 * Blockscout이 받는 컴파일러 버전 문자열.
 *
 * build-info의 `solcLongVersion` 은 두 가지로 나온다.
 *
 *   네이티브 solc  0.8.28+commit.7893614a
 *   solcjs        0.8.28+commit.7893614a.Emscripten.clang
 *
 * 익스플로러는 앞의 형태에 `v` 를 붙인 것만 안다. 뒤쪽 꼬리를 떼어낸다.
 * (바이트코드는 두 빌드가 동일하므로 검증에는 영향이 없다.)
 */
function compilerVersion(buildInfo) {
  const long = buildInfo.solcLongVersion || buildInfo.solcVersion || "";
  const match = long.match(/^(\d+\.\d+\.\d+\+commit\.[0-9a-f]+)/);
  return `v${match ? match[1] : long}`;
}

module.exports = { minimalInput, compilerVersion };
