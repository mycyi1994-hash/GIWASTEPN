package com.giwa.strideup.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giwa.strideup.domain.RewardEconomy
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "strideup_prefs")

/**
 * 사용자 설정과 가벼운 상태(DataStore Preferences).
 * 포인트 잔액은 Room의 rewards 원장 합계로 관리하고, 여기에는
 * 목표/스니커즈 레벨/에너지/스트릭/걸음 기준점만 저장한다.
 */
class UserPrefs(private val context: Context) {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val SNEAKER_LEVEL = intPreferencesKey("sneaker_level")
        val ENERGY = doublePreferencesKey("energy_remaining")
        val ENERGY_DAY = longPreferencesKey("energy_day")
        val STREAK = intPreferencesKey("streak")
        val LAST_GOAL_MET_DAY = longPreferencesKey("last_goal_met_day")
        val BASELINE_DAY = longPreferencesKey("baseline_day")
        val BASELINE_STEPS = longPreferencesKey("baseline_steps")
        val RUNNER_UID = stringPreferencesKey("runner_uid")
        val AVATAR_ID = intPreferencesKey("avatar_id")
        val AVATAR_REV = intPreferencesKey("avatar_rev")
        val LOGIN_METHOD = stringPreferencesKey("login_method")
        val GUIDE_SEEN = intPreferencesKey("guide_seen")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_GOAL] ?: DEFAULT_GOAL }

    // ── 러너 식별 · 프로필 ───────────────────────────────────

    /** 러너 고유 ID — "SU-XXXXXX". 발급 전이면 빈 문자열. */
    val runnerUid: Flow<String> = context.dataStore.data.map { it[Keys.RUNNER_UID] ?: "" }

    /** 선택한 아바타 인덱스 (기본 0, [AVATAR_CUSTOM]이면 갤러리 사진) */
    val avatarId: Flow<Int> = context.dataStore.data.map { it[Keys.AVATAR_ID] ?: 0 }

    /** 갤러리 사진이 바뀔 때마다 올라가는 리비전 — UI가 파일을 다시 읽는 신호 */
    val avatarRev: Flow<Int> = context.dataStore.data.map { it[Keys.AVATAR_REV] ?: 0 }

    suspend fun setAvatarId(id: Int) {
        context.dataStore.edit { it[Keys.AVATAR_ID] = id }
    }

    suspend fun bumpAvatarRev() {
        context.dataStore.edit { it[Keys.AVATAR_REV] = (it[Keys.AVATAR_REV] ?: 0) + 1 }
    }

    /** 로그인 방식 — "google" / "guest" / ""(미선택) */
    val loginMethod: Flow<String> = context.dataStore.data.map { it[Keys.LOGIN_METHOD] ?: "" }

    suspend fun setLoginMethod(method: String) {
        context.dataStore.edit { it[Keys.LOGIN_METHOD] = method }
    }

    /** 앱 언어 태그. 빈 문자열이면 기기 설정을 따른다 */
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "" }

    suspend fun setLanguage(tag: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = tag }
    }

    suspend fun languageNow(): String = context.dataStore.data.first()[Keys.LANGUAGE] ?: ""

    /** 온보딩 가이드를 끝까지 봤는지 */
    val guideSeen: Flow<Boolean> = context.dataStore.data.map { (it[Keys.GUIDE_SEEN] ?: 0) == 1 }

    suspend fun setGuideSeen() {
        context.dataStore.edit { it[Keys.GUIDE_SEEN] = 1 }
    }

    /**
     * 첫 실행 시 러너 UID를 발급한다. 이미 있으면 그 값을 반환한다.
     * 헷갈리는 문자(0/O, 1/I)를 뺀 32문자 알파벳을 쓴다.
     */
    suspend fun ensureRunnerUid(): String {
        val existing = context.dataStore.data.first()[Keys.RUNNER_UID]
        if (!existing.isNullOrBlank()) return existing
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = java.security.SecureRandom()
        val body = buildString {
            repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
        val uid = "SU-$body"
        context.dataStore.edit { it[Keys.RUNNER_UID] = uid }
        return uid
    }

    val sneakerLevel: Flow<Int> = context.dataStore.data.map { it[Keys.SNEAKER_LEVEL] ?: 1 }

    val streak: Flow<Int> = context.dataStore.data.map { it[Keys.STREAK] ?: 0 }

    /**
     * 표시용 에너지. 저장된 날짜가 오늘이 아니면 아직 소모가 없는 것이므로
     * 최대치(= 자정 리필 후 값)로 보여준다. 실제 저장값 갱신은 [currentEnergy]가 담당.
     */
    val energy: Flow<Double> = context.dataStore.data.map { prefs ->
        val level = prefs[Keys.SNEAKER_LEVEL] ?: 1
        val max = RewardEconomy.maxEnergy(level)
        val day = prefs[Keys.ENERGY_DAY] ?: -1L
        if (day != LocalDate.now().toEpochDay()) max else (prefs[Keys.ENERGY] ?: max).coerceIn(0.0, max)
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal }
    }

    suspend fun setSneakerLevel(level: Int) {
        context.dataStore.edit { it[Keys.SNEAKER_LEVEL] = level }
    }

    /** 오늘 남은 에너지를 반환한다. 날짜가 바뀌었으면 최대치로 리필해 저장한다. */
    suspend fun currentEnergy(today: Long): Double {
        val prefs = context.dataStore.data.first()
        val level = prefs[Keys.SNEAKER_LEVEL] ?: 1
        val max = RewardEconomy.maxEnergy(level)
        val day = prefs[Keys.ENERGY_DAY] ?: -1L
        return if (day != today) {
            context.dataStore.edit {
                it[Keys.ENERGY] = max
                it[Keys.ENERGY_DAY] = today
            }
            max
        } else {
            (prefs[Keys.ENERGY] ?: max).coerceIn(0.0, max)
        }
    }

    suspend fun consumeEnergy(today: Long, amount: Double) {
        val remaining = currentEnergy(today)
        context.dataStore.edit {
            it[Keys.ENERGY] = (remaining - amount).coerceAtLeast(0.0)
            it[Keys.ENERGY_DAY] = today
        }
    }

    /** 에너지 셀 등으로 에너지를 회복한다. 최대치를 넘지 않는다. */
    suspend fun restoreEnergy(today: Long, amount: Double) {
        val remaining = currentEnergy(today)
        val level = context.dataStore.data.first()[Keys.SNEAKER_LEVEL] ?: 1
        val max = RewardEconomy.maxEnergy(level)
        context.dataStore.edit {
            it[Keys.ENERGY] = (remaining + amount).coerceIn(0.0, max)
            it[Keys.ENERGY_DAY] = today
        }
    }

    suspend fun streakValue(): Int = context.dataStore.data.first()[Keys.STREAK] ?: 0

    suspend fun lastGoalMetDay(): Long = context.dataStore.data.first()[Keys.LAST_GOAL_MET_DAY] ?: -1L

    suspend fun setGoalMet(day: Long, newStreak: Int) {
        context.dataStore.edit {
            it[Keys.LAST_GOAL_MET_DAY] = day
            it[Keys.STREAK] = newStreak
        }
    }

    /** 걸음 센서 기준점 (기준 날짜 epochDay, 그 시점의 센서 누적값) */
    suspend fun baseline(): Pair<Long, Long> {
        val prefs = context.dataStore.data.first()
        return (prefs[Keys.BASELINE_DAY] ?: -1L) to (prefs[Keys.BASELINE_STEPS] ?: -1L)
    }

    suspend fun setBaseline(day: Long, steps: Long) {
        context.dataStore.edit {
            it[Keys.BASELINE_DAY] = day
            it[Keys.BASELINE_STEPS] = steps
        }
    }

    companion object {
        const val DEFAULT_GOAL = 8000
        const val MIN_GOAL = 3000
        const val MAX_GOAL = 20000

        /** avatarId가 이 값이면 갤러리에서 고른 사진을 쓴다 */
        const val AVATAR_CUSTOM = -2

        /** 갤러리 아바타 저장 파일명 (filesDir) */
        const val AVATAR_FILE = "avatar_custom.jpg"
    }
}
