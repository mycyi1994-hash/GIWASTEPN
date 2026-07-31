# StepUp Attester

러닝 세션을 검사하고 EIP-712로 서명해, `RewardDistributor`가 받아들일 청구서를
만드는 서비스. Cloudflare Worker 하나로 돌아간다.

**테스트 7개 통과** · viem · 무료 티어로 하루 10만 요청

---

## 이 서비스가 있는 이유

컨트랙트는 "정말 뛰었는가"를 알 수 없다. GPS 좌표 수백 개를 체인에 올려 검사하는
것은 가스비로 불가능하고, 부정 탐지 규칙은 계속 바뀌기 때문에 불변 코드에 넣을
것도 아니다.

그래서 역할을 나눴다.

| | 누가 정하나 | 왜 거기인가 |
|---|---|---|
| **누구에게 얼마** | 어테스터 (여기) | 부정 탐지는 계속 고쳐야 한다 |
| **얼마나 존재 가능한가** | `RewardDistributor` | 공급 상한은 절대 안 바뀐다 |

**이 키가 통째로 털려도 토큰은 인플레이션되지 않는다.** 모든 청구가 컨트랙트의
일일 예산에서 차감되므로, 최악의 경우 하루치 배출이 잘못 나갈 뿐이다. 그게
서명 서비스를 밖에 둘 수 있는 이유다.

---

## 핵심 원칙 — 클라이언트를 믿지 않는다

앱은 고쳐서 다시 설치할 수 있고, 세션 레코드는 조작할 수 있다. 그래서 이 서비스는
**앱이 보낸 금액을 참고조차 하지 않는다.** 원본 GPS 좌표를 받아 직접 다시 계산한다.

| 검사 | 내용 |
|---|---|
| 구간 속도 | 두 점 사이가 25 km/h를 넘으면 그 구간의 거리를 버린다 |
| 세션 판정 | 튄 구간이 절반 넘고 3회 이상이면 **VOID** — 서명하지 않는다 |
| 케이던스 | 분당 240보 초과면 GPS와 무관하게 VOID |
| 걸음↔GPS 교차검증 | 걸음으로 잰 거리와 GPS 거리가 2배 이상 어긋나면 거부 |
| 세션 길이 | 60초 미만은 러닝으로 보지 않는다 |
| 재사용 | 세션 해시를 서버가 다시 만들고, 체인에서 기청구 여부를 확인 |
| 예산 | `dayRemaining`이 모자라면 서명하지 않는다 (헛된 트랜잭션 방지) |

판정 상수는 [`src/economy.js`](src/economy.js)에 모여 있고, 안드로이드의
`domain/RunIntegrity.kt`·`domain/RewardEconomy.kt`와 **같은 값**이다.
한쪽만 고치면 앱이 보여준 금액과 체인 금액이 달라진다 — 유저가 가장 먼저
알아채는 종류의 버그다. 테스트가 그 일치를 지킨다.

---

## 배포 (5분)

> **선행 조건 — 이미 충족됐다.** 컨트랙트 4종은 2026-07-31에 GIWA Sepolia
> (91342)에 배포됐고, `wrangler.toml`의 `DISTRIBUTOR_ADDRESS`도 실제 주소
> [`0x9f9E87bD…aCFE36E1`](https://sepolia-explorer.giwa.io/address/0x9f9E87bD825144A8315d30979E3004FbaCFE36E1)
> 로 채워져 있다. 3번 항목은 건너뛰어도 된다.

### 1. 어테스터 전용 지갑 만들기

배포자 지갑과 **달라야 한다.** 이 키는 클라우드에 올라가고, 배포자 키는 올라가면
안 된다.

MetaMask → 계정 추가 → 새 계정 → 이름 "StepUp 어테스터" → 개인 키 표시 → 복사

> 이 계정에는 **가스비가 필요 없다.** 서명만 하고 트랜잭션은 보내지 않는다.

### 2. 컨트랙트에 어테스터 주소 알려주기

> **지금 배포된 컨트랙트는 이 단계가 필요하다.** 2026-07-31 배포 시
> `ATTESTER_ADDRESS`를 지정하지 않아 **어테스터가 배포자 계정과 같다.**
> 1번에서 만든 전용 지갑으로 반드시 바꿔야 한다 — 안 그러면 배포자 개인키를
> 클라우드에 올리게 된다.

배포할 때 `ATTESTER_ADDRESS`로 넣었다면 이미 끝났다. 아니라면 배포자 계정으로
한 번 호출한다.

```
https://sepolia-explorer.giwa.io/address/0x9f9E87bD825144A8315d30979E3004FbaCFE36E1?tab=write_contract
  → Connect Wallet (배포자 계정으로)
  → setAttester(<1번에서 만든 어테스터 주소>)
```

### 3. Worker 설정

```bash
cd attester
npm install
```

`wrangler.toml`의 `DISTRIBUTOR_ADDRESS`는 이미 배포된 주소로 채워져 있다.
다른 네트워크에 다시 배포했을 때만 손대면 된다.

### 4. 서명 키를 Secret으로

```bash
npx wrangler secret put ATTESTER_PRIVATE_KEY
```

붙여넣기 프롬프트가 뜬다. **Secret은 대시보드에서도 다시 볼 수 없고 저장소에도
남지 않는다.** `wrangler.toml`의 `[vars]`에 넣으면 안 된다 — 그건 커밋된다.

### 5. 배포

```bash
npm run deploy
```

`https://stepup-attester.<계정>.workers.dev` 가 나온다.

### 6. 확인

```bash
curl https://stepup-attester.<계정>.workers.dev/health
```

```json
{
  "ok": true,
  "chain": 91342,
  "distributor": "0x...",
  "attester": "0x..."
}
```

`attester` 주소가 컨트랙트의 `attester()`와 같은지 확인한다. 다르면 청구가
`BadSignature`로 되돌아온다.

---

## API

### `POST /claim`

```json
{
  "runner": "0xRunnerAddress",
  "startedAt": 1767200000000,
  "endedAt": 1767201800000,
  "steps": 3000,
  "boostBps": 330,
  "partySize": 1,
  "track": [
    { "lat": 37.5265, "lng": 126.9245, "t": 1767200000000 },
    { "lat": 37.5271, "lng": 126.9251, "t": 1767200010000 }
  ]
}
```

성공하면 앱이 그대로 `RewardDistributor.claim(claim, signature)`에 넣을 수 있는
형태로 돌려준다.

```json
{
  "ok": true,
  "verdict": "CLEAN",
  "claim": {
    "runner": "0x…",
    "sessionHash": "0x…",
    "amount": "30000000000000000000",
    "day": 3,
    "deadline": 1767202400
  },
  "signature": "0x…",
  "inspection": { "validSegments": 180, "flaggedSegments": 0, "validMeters": 2286, "topSpeedKmh": 10.4 }
}
```

| 상태 | 뜻 |
|---|---|
| `400` | 형식 오류 (주소·시간·걸음 범위) |
| `409` | 이미 청구된 세션 |
| `422` | 러닝으로 확인되지 않음 (VOID) |
| `429` | 오늘 배출 예산 소진 |
| `502` | 체인 조회 실패 |
| `503` | 어테스터 미설정 |

### `GET /health`

설정 상태와 어테스터 주소를 돌려준다.

---

## 테스트

```bash
npm test
```

앱의 `RunIntegrityTest`와 같은 경계를 서버에서도 지키는지 확인한다 — 조깅은
통과, 차 속도는 버림, 절반 넘게 튀면 VOID, 지급액이 앱 계산과 정확히 일치.

---

## 아직 없는 것

정직하게 적는다.

- **기기 증명(Play Integrity)** — 루팅·에뮬레이터 기기를 걸러내지 않는다.
  GPS 위조 앱은 아직 이 서비스를 속일 수 있다.
- **레이트 리밋** — 같은 러너가 초당 수십 번 두드리는 것을 막지 않는다.
  Cloudflare Rate Limiting 규칙으로 붙일 자리다.
- **어테스터 다중화** — 키 하나가 단일 신뢰 지점이다. 임계 서명으로 가는 것이
  메인넷 이전 마일스톤이다.

이 셋은 [`../docs/TOKENOMICS.md`](../docs/TOKENOMICS.md) §8·§9에 미결 과제로
적어 둔 것과 같은 항목이다.
