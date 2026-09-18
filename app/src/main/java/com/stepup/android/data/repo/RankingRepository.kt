package com.stepup.android.data.repo

import com.stepup.android.data.remote.FactionRankRow
import com.stepup.android.data.remote.LeaderboardRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.domain.Faction
import com.stepup.android.domain.FactionRank
import com.stepup.android.domain.RankBoard
import com.stepup.android.domain.RankEntry
import com.stepup.android.domain.RankPeriod

/** 순위를 못 가져온 이유. 화면이 어떤 말을 할지 정하는 데 쓴다. */
enum class RankingProblem {
    /** 지금은 안 되지만 나중에는 된다 — 연결이 없거나 서버가 바쁘다 */
    OFFLINE,

    /** 다시 로그인해야 한다 */
    SIGN_IN_REQUIRED,

    /** 서버가 거절했다. 다시 시도해도 같다. */
    REJECTED,
}

sealed interface RankingState {
    data object Loading : RankingState

    data class Failed(val problem: RankingProblem) : RankingState

    /**
     * @property entries 상위 몇 명과 내 줄. 아직 한 번도 안 뛰었으면 내 줄이 없다.
     * @property totalRunners 순위에 오른 전체 인원. 받은 줄 수가 아니다.
     */
    data class Ready(val entries: List<RankEntry>, val totalRunners: Int) : RankingState {
        val me: RankEntry? get() = entries.firstOrNull { it.isMe }
    }
}

sealed interface FactionRankingState {
    data object Loading : FactionRankingState

    data class Failed(val problem: RankingProblem) : FactionRankingState

    data class Ready(val rows: List<FactionRank>) : FactionRankingState
}

/**
 * 순위표를 서버에서 가져온다.
 *
 * 예전에는 상대 15명을 코드에 박아 두고 내 기록만 끼워 넣었다. 화면이 하려던
 * 말은 "당신은 몇 등입니다"인데 상대가 가짜면 그 말은 거짓이다. 그래서
 * 못 가져왔을 때 지어내지 않고 못 가져왔다고 말한다 — 빈 순위표가 거짓말보다 낫다.
 *
 * 순위를 서버가 계산하는 데는 다른 이유도 있다. 앱은 남의 기록을 볼 수 없다
 * (RLS). 볼 수 있게 열면 순위를 위해 모두의 러닝 기록을 모두에게 공개하는
 * 셈이 된다. 서버 함수는 합계만 내보낸다.
 */
class RankingRepository(private val server: StepUpServer) {

    /**
     * @param period 셀 기간. 전체기간만 있으면 순위표는 일찍 시작한 사람의
     *   명단이 되고, 어제 가입한 사람은 두 번 보지 않는다.
     * @param meLabel 내 줄에 쓸 이름. 순위표에서 "나"를 찾는 데 1초도 쓰지
     *   않게 하려는 것이다 — 자기 이름을 목록에서 찾는 것은 생각보다 느리다.
     */
    suspend fun personal(
        board: RankBoard,
        period: RankPeriod,
        meLabel: String,
        limit: Int = 20,
    ): RankingState =
        when (val result = server.leaderboard(board.name, limit, period.name)) {
            is ServerResult.Ok -> RankingState.Ready(
                entries = result.value.map { it.toEntry(meLabel) },
                totalRunners = result.value.firstOrNull()?.total ?: 0,
            )
            is ServerResult.Retry -> RankingState.Failed(RankingProblem.OFFLINE)
            is ServerResult.SignInRequired -> RankingState.Failed(RankingProblem.SIGN_IN_REQUIRED)
            is ServerResult.Rejected -> RankingState.Failed(RankingProblem.REJECTED)
        }

    /** @param myFaction 지금 신고 있는 신발의 종족. "우리 편"을 표시하는 데 쓴다. */
    suspend fun factions(myFaction: Faction?, period: RankPeriod): FactionRankingState =
        when (val result = server.factionLeaderboard(period.name)) {
            is ServerResult.Ok -> FactionRankingState.Ready(
                result.value.toRanks(myFaction),
            )
            is ServerResult.Retry -> FactionRankingState.Failed(RankingProblem.OFFLINE)
            is ServerResult.SignInRequired ->
                FactionRankingState.Failed(RankingProblem.SIGN_IN_REQUIRED)
            is ServerResult.Rejected -> FactionRankingState.Failed(RankingProblem.REJECTED)
        }
}

private fun LeaderboardRow.toEntry(meLabel: String): RankEntry = RankEntry(
    rank = rank,
    name = if (isMe) meLabel else name,
    monogram = if (isMe) "ME" else monogram,
    topSpeedKmh = topSpeedKmh,
    activeSec = activeSec,
    sup = sup,
    isMe = isMe,
)

/**
 * 서버가 보낸 종족 줄을 화면이 쓰는 모양으로.
 *
 * 서버는 거리순으로 보내지만 순위 번호는 붙이지 않는다. 같은 거리가 둘이면
 * 번호를 어떻게 매길지는 화면의 문제이지 서버의 문제가 아니다.
 */
private fun List<FactionRankRow>.toRanks(myFaction: Faction?): List<FactionRank> =
    // 모르는 종족을 **먼저** 걸러 낸다. 번호를 매긴 뒤에 걸러 내면 1등이
    // 사라진 자리에 2등이 남아, 화면에 2·3·4 등만 늘어선다. 서버에 새 종족이
    // 먼저 생기고 앱이 나중에 따라가는 일은 실제로 일어난다.
    mapNotNull { row ->
        val faction = Faction.entries.firstOrNull { it.id == row.faction }
        if (faction == null) null else faction to row
    }
        .sortedByDescending { (_, row) -> row.km }
        .mapIndexed { index, (faction, row) ->
            FactionRank(
                rank = index + 1,
                faction = faction,
                km = row.km,
                myKm = row.myKm,
                isMine = faction == myFaction,
            )
        }
