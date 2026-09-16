# 릴리즈 서명 설정 (Release signing)

릴리즈 AAB에 서명하려면 업로드 키가 필요합니다. CI(`.github/workflows/release-aab.yml`)와
`app/build.gradle.kts`가 아래 시크릿을 읽습니다.

> ⚠️ **이 키를 잃어버리면 앱 업데이트를 영원히 올릴 수 없습니다.** 반드시 백업하세요.

시크릿이 없으면 서명 단계는 조용히 건너뜁니다 — 등록 전에도 나머지 빌드는 정상 동작합니다.

## 1. 업로드 키 만들기

```bash
keytool -genkeypair -v \
  -keystore stepup-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias stepup-upload
```

비밀번호를 묻습니다. **비밀번호와 `stepup-upload.jks` 파일을 비밀번호 관리자에
백업**하세요. 저장소에는 절대 커밋하지 않습니다.

## 2. GitHub Secrets에 등록

키 파일을 base64로 바꿉니다.

```bash
# macOS · Linux
base64 -i stepup-upload.jks | tr -d '\n' > keystore.b64
```

```powershell
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("stepup-upload.jks")) > keystore.b64
```

저장소 → **Settings → Secrets and variables → Actions → New repository secret** 로
네 개를 등록합니다.

| 이름 | 값 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `keystore.b64` 파일 내용 전체 |
| `RELEASE_KEYSTORE_PASSWORD` | 1단계의 키스토어 비밀번호 |
| `RELEASE_KEY_ALIAS` | `stepup-upload` |
| `RELEASE_KEY_PASSWORD` | 1단계의 키 비밀번호 (키스토어와 같게 했다면 동일) |

등록하면 CI가 서명된 AAB를 만듭니다.

> Play Console에서 **Play App Signing**을 켜면 구글이 배포 키를 따로 관리합니다.
> 이 업로드 키는 "구글에 올릴 때 쓰는 열쇠"일 뿐이라, 최악의 경우 재발급이 가능합니다.

---

디버그 빌드에 쓰는 `app/debug.keystore`는 의도적으로 저장소에 포함돼 있습니다 —
배경은 [README.ko.md](../README.ko.md#저장소에-포함된-debug-키스토어에-대해) 참고.
