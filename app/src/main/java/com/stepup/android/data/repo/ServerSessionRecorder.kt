package com.stepup.android.data.repo

import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionRecorded
import com.stepup.android.data.remote.StepUpServer

/**
 * 저장된 세션 한 줄을 서버 호출로 옮긴다.
 *
 * 저장소(Room)의 모양과 서버가 받는 모양을 갈라 두기 위한 얇은 층이다.
 * 둘 중 하나가 바뀌어도 다른 쪽이 따라 흔들리지 않는다.
 */
class ServerSessionRecorder(private val server: StepUpServer) : SessionRecorder {

    override val isConfigured: Boolean get() = server.isConfigured

    override suspend fun record(session: WalkSessionEntity): ServerResult<SessionRecorded> =
        server.recordSession(
            startedAtMillis = session.startedAt,
            endedAtMillis = session.endedAt,
            steps = session.steps,
            durationSec = session.durationSec,
            track = session.track,
            boostBps = session.boostBps,
            partySize = session.partySize,
            faction = session.faction,
        )
}
