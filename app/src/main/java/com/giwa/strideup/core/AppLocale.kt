package com.giwa.strideup.core

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 앱 언어.
 *
 * 사용자가 고른 언어는 DataStore에 저장하고, 실제 적용은 두 갈래로 나뉜다.
 *
 * - Android 13(API 33) 이상: 시스템의 "앱별 언어" 기능([LocaleManager])에 넘긴다.
 *   OS가 저장·적용·화면 재생성까지 맡고, 시스템 설정 화면과도 값이 동기화된다.
 * - 그 아래: 프로세스의 기본 로케일과 앱 리소스 설정을 직접 바꾸고,
 *   액티비티를 다시 만들어 화면 전체를 새 언어로 그린다.
 *
 * 어느 쪽이든 [Context.getString]이 새 언어를 따르므로, 화면뿐 아니라
 * 저장소가 시드하는 문구·알림 문구도 같은 언어로 나온다.
 */
object AppLocale {

    /** 지원 언어 — 시스템 기본은 빈 태그로 표현한다 */
    const val SYSTEM = ""

    val SUPPORTED: List<String> = listOf("en", "ko", "zh", "ja")

    /** 현재 선택된 언어 태그. 빈 문자열이면 시스템 기본 */
    @Volatile
    var tag: String = SYSTEM
        private set

    /** 앱 시작 시 저장된 선택을 프로세스에 적용한다 */
    fun bootstrap(context: Context, saved: String) {
        tag = normalize(saved)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            applyToProcess(context, tag)
        }
    }

    /**
     * 언어를 바꾼다. 화면 재생성은 호출한 쪽에서 처리한다
     * (API 33+에서는 OS가 알아서 다시 만든다).
     *
     * @return 호출자가 직접 액티비티를 재생성해야 하면 true
     */
    fun change(context: Context, next: String): Boolean {
        tag = normalize(next)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            false
        } else {
            applyToProcess(context, tag)
            true
        }
    }

    /** 시스템이 기억하고 있는 값을 읽어 [tag]를 맞춘다 (API 33+ 전용) */
    fun syncFromSystem(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        tag = normalize(locales?.takeIf { !it.isEmpty }?.get(0)?.language.orEmpty())
    }

    /** 액티비티가 붙기 전 베이스 컨텍스트를 선택한 언어로 감싼다 (API 33 미만) */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || tag.isEmpty()) return base
        val config = Configuration(base.resources.configuration)
        val locale = Locale.forLanguageTag(tag)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /** 지원하지 않는 태그는 시스템 기본으로 되돌린다 */
    private fun normalize(raw: String): String {
        val language = raw.substringBefore('-').lowercase()
        return if (language in SUPPORTED) language else SYSTEM
    }

    @Suppress("DEPRECATION")
    private fun applyToProcess(context: Context, tag: String) {
        val locale = if (tag.isEmpty()) systemDefault() else Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val resources = context.applicationContext.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        // 저장소들이 쓰는 앱 컨텍스트의 리소스도 같이 갈아끼워야
        // 시드 문구·알림 문구가 같은 언어로 나온다.
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * 사용자가 "시스템 기본"으로 되돌렸을 때 쓸 기기 로케일.
     *
     * [Locale.getDefault]는 우리가 이미 덮어썼을 수 있으므로 쓰면 안 된다.
     * [android.content.res.Resources.getSystem]은 앱이 건드리지 못하는 시스템 리소스라
     * 항상 진짜 기기 설정을 들고 있다.
     */
    private fun systemDefault(): Locale {
        val locales = android.content.res.Resources.getSystem().configuration.locales
        return if (locales.size() > 0) locales.get(0) else Locale.ENGLISH
    }
}
