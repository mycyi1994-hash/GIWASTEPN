package com.giwa.strideup.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giwa.strideup.domain.Faction
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
        val SELECTED_COURSE = longPreferencesKey("selected_course")
        val TOP_SPEED = doublePreferencesKey("top_speed_kmh")
        val FACTION_KM = stringPreferencesKey("faction_km")
        val ACCOUNTED_STEPS = longPreferencesKey("accounted_steps")
        val ACCOUNTED_DAY = longPreferencesKey("accounted_day")
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

    /** 선택한 러닝 코스 id. -1이면 선택 없음 */
    val selectedCourseId: Flow<Long> = context.dataStore.data.map { it[Keys.SELECTED_COURSE] ?: -1L }

    suspend fun setSelectedCourse(id: Long) {
        context.dataStore.edit { it[Keys.SELECTED_COURSE] = id }
    }

    suspend fun selectedCourseNow(): Long = context.dataStore.data.first()[Keys.SELECTED_COURSE] ?: -1L

    // ── 적립 정산 기준점 ─────────────────────────────────────────

    /**
     * 오늘 이미 정산에 반영한 걸음 수.
     *
     * 백그라운드 적립과 러닝 세션 정산이 같은 걸음을 두 번 세지 않게 하는
     * 기준점이다. 세션이 끝나면 그 세션의 걸음이 여기 더해지고, 백그라운드
     * 스윕은 "오늘 걸음 − 이 값"만 정산한다. 날짜가 바뀌면 0에서 시작한다.
     */
    suspend fun accountedStepsToday(today: Long): Int {
        val prefs = context.dataStore.data.first()
        val day = prefs[Keys.ACCOUNTED_DAY] ?: -1L
        if (day != today) return 0
        return (prefs[Keys.ACCOUNTED_STEPS] ?: 0L).toInt()
    }

    /** 정산한 걸음을 기준점에 더한다. 날짜가 바뀌었으면 오늘치부터 다시 센다. */
    suspend fun addAccountedSteps(today: Long, steps: Int) {
        if (steps <= 0) return
        context.dataStore.edit { prefs ->
            val day = prefs[Keys.ACCOUNTED_DAY] ?: -1L
            val base = if (day == today) prefs[Keys.ACCOUNTED_STEPS] ?: 0L else 0L
            prefs[Keys.ACCOUNTED_DAY] = today
            prefs[Keys.ACCOUNTED_STEPS] = base + steps
        }
    }

    /**
     * 기준점을 [floorValue] 이상으로 끌어올린다. 이미 그보다 크면 두고, 절대 내리지 않는다.
     *
     * 세션 정산이 쓰는 경로다. 세션 걸음을 그냥 더하면 두 가지가 깨진다.
     *  - **자정을 넘긴 세션**: 어제 걸은 몫까지 오늘 기준점에 얹혀, 오늘 처음 걷는
     *    그만큼이 통째로 사라진다.
     *  - **일시정지 중 걸음**: 세션 걸음에 안 잡히므로 더하기만으로는 기준점이
     *    모자라고, 그 몫을 백그라운드가 다시 지급한다.
     *
     * 두 경우 모두 "지금 이 순간의 오늘 걸음 수"를 기준점으로 못 박으면 해결된다.
     */
    suspend fun raiseAccountedTo(today: Long, floorValue: Int) {
        if (floorValue <= 0) return
        context.dataStore.edit { prefs ->
            val day = prefs[Keys.ACCOUNTED_DAY] ?: -1L
            val base = if (day == today) prefs[Keys.ACCOUNTED_STEPS] ?: 0L else 0L
            prefs[Keys.ACCOUNTED_DAY] = today
            prefs[Keys.ACCOUNTED_STEPS] = maxOf(base, floorValue.toLong())
        }
    }

    // ── 랭킹 재료 ────────────────────────────────────────────────

    /** 역대 최고 속도(km/h). 러닝 판정을 통과한 구간에서만 갱신된다. */
    val topSpeedKmh: Flow<Double> = context.dataStore.data.map { it[Keys.TOP_SPEED] ?: 0.0 }

    suspend fun recordTopSpeed(kmh: Double) {
        if (kmh <= 0.0) return
        context.dataStore.edit { prefs ->
            val best = prefs[Keys.TOP_SPEED] ?: 0.0
            if (kmh > best) prefs[Keys.TOP_SPEED] = kmh
        }
    }

    /**
     * 종족별 누적 러닝 거리(km) — 착용한 신발의 종족에 쌓인다.
     *
     * Faction.entries 순서대로 ";"로 이어 붙인 문자열 하나로 보관한다.
     * 종족이 늘어도 키를 새로 파지 않아도 되고, 짧아진 문자열은 0으로 채운다.
     */
    val factionKm: Flow<Map<Faction, Double>> = context.dataStore.data.map { prefs ->
        decodeFactionKm(prefs[Keys.FACTION_KM])
    }

    suspend fun addFactionKm(faction: Faction, km: Double) {
        if (km <= 0.0) return
        context.dataStore.edit { prefs ->
            val current = decodeFactionKm(prefs[Keys.FACTION_KM]).toMutableMap()
            current[faction] = (current[faction] ?: 0.0) + km
            prefs[Keys.FACTION_KM] = Faction.entries.joinToString(";") {
                "%.4f".format(current[it] ?: 0.0)
            }
        }
    }

    private fun decodeFactionKm(raw: String?): Map<Faction, Double> {
        val parts = raw?.split(';').orEmpty()
        return Faction.entries.withIndex().associate { (index, faction) ->
            faction to (parts.getOrNull(index)?.toDoubleOrNull() ?: 0.0)
        }
    }

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

    /**
     * 에너지를 소모한다.
     *
     * 읽기와 쓰기를 한 [edit] 블록 안에서 끝낸다. DataStore는 edit를 직렬화하므로
     * 이렇게 해야 원자적이다. 백그라운드 적립이 생기면서 세션 정산과 동시에
     * 에너지를 건드리는 일이 일상이 됐고, 바깥에서 읽고 안에서 쓰면 갱신이
     * 유실되어 상한이 새거나 구매한 에너지 셀이 사라진다.
     */
    suspend fun consumeEnergy(today: Long, amount: Double) {
        context.dataStore.edit { prefs ->
            val remaining = energyIn(prefs, today)
            prefs[Keys.ENERGY] = (remaining - amount).coerceAtLeast(0.0)
            prefs[Keys.ENERGY_DAY] = today
        }
    }

    /** 에너지 셀 등으로 에너지를 회복한다. 최대치를 넘지 않는다. */
    suspend fun restoreEnergy(today: Long, amount: Double) {
        context.dataStore.edit { prefs ->
            val remaining = energyIn(prefs, today)
            val level = prefs[Keys.SNEAKER_LEVEL] ?: 1
            val max = RewardEconomy.maxEnergy(level)
            prefs[Keys.ENERGY] = (remaining + amount).coerceIn(0.0, max)
            prefs[Keys.ENERGY_DAY] = today
        }
    }

    /** 자정 리필을 반영한 현재 에너지. [consumeEnergy]/[restoreEnergy]가 edit 안에서 쓴다. */
    private fun energyIn(prefs: Preferences, today: Long): Double {
        val day = prefs[Keys.ENERGY_DAY] ?: -1L
        val level = prefs[Keys.SNEAKER_LEVEL] ?: 1
        val max = RewardEconomy.maxEnergy(level)
        return if (day != today) max else (prefs[Keys.ENERGY] ?: max).coerceIn(0.0, max)
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
