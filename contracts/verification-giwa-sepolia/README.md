# GIWA Sepolia 수동 검증 파일

익스플로러 웹 화면에서 **터미널 없이** 소스 검증을 끝낼 수 있도록 미리 뽑아 둔
Standard JSON Input 파일입니다.

| | |
|---|---|
| 생성 시각 | 2026-09-16 |
| 생성 명령 | `npm run standard-json:giwa` |
| 대상 | 아래 4개 주소 (GIWA Sepolia, 체인 ID 91342) |
| 컴파일러 | `v0.8.28+commit.7893614a` |
| 최적화 | **Yes** / runs **200** |
| EVM 버전 | `cancun` |

> ⚠️ 이 파일들은 **현재 배포된 바이트코드에 대응합니다.** 컨트랙트 소스를 고치면
> 더 이상 맞지 않으니 `npm run standard-json:giwa` 로 다시 뽑으세요.
> (평소에는 `contracts/verification/` 에 생성되며 그 경로는 gitignore 대상입니다.)

---

## 하는 법

각 컨트랙트마다 아래를 반복합니다. 4번 하면 끝납니다.

1. **검증 페이지**를 엽니다 (표의 링크).
2. **Verification method** → `Solidity (Standard JSON Input)`
3. **Compiler** → `v0.8.28+commit.7893614a`
4. **Optimization** → `Yes`, **runs** → `200`
5. 이 폴더의 **JSON 파일을 업로드**합니다.
6. **Constructor arguments**(ABI-encoded) → 아래 hex를 그대로 붙여넣습니다.
7. **Verify & publish** → 성공하면 주소 페이지에 **`Code` 탭**이 생깁니다.

---

## 1. SUPToken

- 주소 [`0xb052A8f6A5034747902b6d6787bbfF31A9006c1B`](https://sepolia-explorer.giwa.io/address/0xb052A8f6A5034747902b6d6787bbfF31A9006c1B)
- [검증 페이지 열기](https://sepolia-explorer.giwa.io/address/0xb052A8f6A5034747902b6d6787bbfF31A9006c1B/contract-verification)
- 파일 `SUPToken.standard-input.json`
- 컨트랙트 이름 `contracts/SUPToken.sol:SUPToken`
- 생성자 인자

```
0x000000000000000000000000e96a75e86e25bf4c3d56b17ecfad5aee39046ff7
```

## 2. SneakerNFT

- 주소 [`0x8174f905d86438ac8922c85d3A48604BabEFc960`](https://sepolia-explorer.giwa.io/address/0x8174f905d86438ac8922c85d3A48604BabEFc960)
- [검증 페이지 열기](https://sepolia-explorer.giwa.io/address/0x8174f905d86438ac8922c85d3A48604BabEFc960/contract-verification)
- 파일 `SneakerNFT.standard-input.json`
- 컨트랙트 이름 `contracts/SneakerNFT.sol:SneakerNFT`
- 생성자 인자 (한 줄입니다 — 줄바꿈 없이 전체를 복사하세요)

```
0x000000000000000000000000b052a8f6a5034747902b6d6787bbff31a9006c1b000000000000000000000000e96a75e86e25bf4c3d56b17ecfad5aee39046ff7000000000000000000000000e96a75e86e25bf4c3d56b17ecfad5aee39046ff700000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000018697066733a2f2f5245504c4143455f574954485f4349442f0000000000000000
```

## 3. RewardDistributor

- 주소 [`0x9f9E87bD825144A8315d30979E3004FbaCFE36E1`](https://sepolia-explorer.giwa.io/address/0x9f9E87bD825144A8315d30979E3004FbaCFE36E1)
- [검증 페이지 열기](https://sepolia-explorer.giwa.io/address/0x9f9E87bD825144A8315d30979E3004FbaCFE36E1/contract-verification)
- 파일 `RewardDistributor.standard-input.json`
- 컨트랙트 이름 `contracts/RewardDistributor.sol:RewardDistributor`
- 생성자 인자

```
0x000000000000000000000000b052a8f6a5034747902b6d6787bbff31a9006c1b000000000000000000000000e96a75e86e25bf4c3d56b17ecfad5aee39046ff7
```

## 4. CourseRegistry

- 주소 [`0x6c815DF0d8a5CA7CA0487D1AC2f96c0fEC588542`](https://sepolia-explorer.giwa.io/address/0x6c815DF0d8a5CA7CA0487D1AC2f96c0fEC588542)
- [검증 페이지 열기](https://sepolia-explorer.giwa.io/address/0x6c815DF0d8a5CA7CA0487D1AC2f96c0fEC588542/contract-verification)
- 파일 `CourseRegistry.standard-input.json`
- 컨트랙트 이름 `contracts/CourseRegistry.sol:CourseRegistry`
- 생성자 인자

```
0x000000000000000000000000e96a75e86e25bf4c3d56b17ecfad5aee39046ff7
```

---

## 자동으로 하고 싶다면

PC에 Node.js가 있으면 한 줄로 끝납니다. 이쪽이 더 빠릅니다.

```bash
cd contracts && npm install && npm run verify:giwa
```

실패하면 에러 메시지를 그대로 알려 주세요. 위 수동 절차는 그때를 위한 대비책입니다.
