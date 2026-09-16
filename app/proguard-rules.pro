# StepUp — R8 / ProGuard 규칙
#
# release 빌드는 minify + resource shrink가 켜져 있다. 아래 규칙은
# 라이브러리가 제공하는 consumer 규칙으로 덮이지 않는 부분만 채운다.
#
# 규칙을 추가할 때는 "왜 필요한가"를 반드시 적는다. 이유 없는 keep이 쌓이면
# 난독화가 무의미해지고, 나중에 지워도 되는지 아무도 모르게 된다.

# ── 크래시 리포트를 읽을 수 있게 ────────────────────────────────────
# 줄 번호가 없으면 Crashlytics 스택트레이스가 쓸모없어진다.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── 코루틴 ──────────────────────────────────────────────────────────
# 예외 스택트레이스 복원에 쓰이는 내부 심볼. 없으면 비동기 크래시의
# 원인 지점이 사라진다.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Room ────────────────────────────────────────────────────────────
# Room은 consumer 규칙으로 생성 클래스를 지키지만, 엔티티는 DB 컬럼명이
# 필드명에서 나오므로 이름이 바뀌면 안 된다.
-keep class com.stepup.android.data.local.** { *; }

# ── 안드로이드 컴포넌트 ─────────────────────────────────────────────
# 매니페스트에 이름으로 적힌 클래스는 매니페스트 병합이 지켜 주지만,
# Intent(this, X::class.java) 로만 참조되는 서비스는 명시해 둔다.
-keep class com.stepup.android.service.WalkSessionService { *; }

# ── Compose ─────────────────────────────────────────────────────────
# Compose 컴파일러/런타임은 자체 consumer 규칙을 제공한다. 추가 keep 불필요.

# ── 경고 억제 ───────────────────────────────────────────────────────
# JSR-305 애너테이션(코루틴이 참조)은 런타임에 없어도 무방하다.
-dontwarn javax.annotation.**
