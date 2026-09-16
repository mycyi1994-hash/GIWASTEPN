package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.UploadState
import com.giwa.strideup.data.local.WalkSessionDao
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.data.remote.ClaimRequest
import com.giwa.strideup.data.remote.ClaimResult
import com.giwa.strideup.data.remote.ClaimSubmitter
import com.giwa.strideup.data.remote.toDto
import com.giwa.strideup.domain.RunTrack
import kotlinx.coroutines.flow.Flow

/**
 * 한 번 돌린 업로드의 결말.
 *
 * 일꾼(Worker)이 "다시 깨워야 하는가"를 판단하는 근거다.
 */
data class UploadRun(
    val signed: Int = 0,
    val rejected: Int = 0,
    val failed: Int = 0,
    /** 다시 시도할 가치가 있는 세션이 남았는가 */
    val shouldRetry: Boolean = false,
    /** 아예 시도하지 못한 이유 (지갑 없음, 서버 주소 없음 등) */
    val blockedBy: String? = null,
)

/**
 * 쌓인 러닝 세션을 증명 서버로 올린다.
 *
 * 러닝이 끝나는 곳은 지하철이거나 산이다. 그 자리에서 바로 보내는 것을
 * 전제로 만들면 기록이 사라진다. 그래서 세션은 일단 기기에 쌓이고, 이
 * 저장소가 연결이 될 때 대신 밀어 올린다.
 */
class ClaimRepository(
    private val sessionDao: WalkSessionDao,
    private val client: ClaimSubmitter,
    /** 보상을 받을 지갑 주소. 지갑이 붙기 전에는 비어 있다. */
    private val runnerAddress: suspend () -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** 올릴 것이 몇 개 남았는지 — 화면에 보여주기 위한 값 */
    fun observePendingCount(): Flow<Int> = sessionDao.observePendingUploadCount()

    /**
     * 대기열을 한 번 훑는다.
     *
     * 한 번에 [limit] 개까지만 처리한다. 오래 돌수록 안드로이드가 일꾼을
     * 중간에 끊을 확률이 커지고, 끊기면 어디까지 했는지가 애매해진다.
     * 남은 것은 다음 차례에 이어서 한다.
     */
    suspend fun uploadPending(limit: Int = 10): UploadRun {
        if (!client.isConfigured) {
            return UploadRun(blockedBy = "증명 서버 주소가 아직 설정되지 않았습니다")
        }
        val runner = runnerAddress()
        if (runner.isBlank()) {
            // 지갑이 없으면 보상을 받을 주소가 없다. 세션은 그대로 쌓아 둔다 —
            // 나중에 지갑을 만들면 그동안 뛴 것이 살아 있어야 한다.
            return UploadRun(blockedBy = "지갑이 아직 연결되지 않았습니다")
        }

        val pending = sessionDao.pendingUploads(limit)
        if (pending.isEmpty()) return UploadRun()

        var signed = 0
        var rejected = 0
        var failed = 0

        for (session in pending) {
            val track = RunTrack.decode(session.track)
            if (track.size < 2) {
                // 서버는 좌표 2개 이상을 요구한다. 보내봐야 400이므로 여기서 끊는다.
                sessionDao.update(session.rejected("GPS 경로가 충분하지 않습니다"))
                rejected++
                continue
            }

            val result = client.submit(
                ClaimRequest(
                    runner = runner,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    steps = session.steps,
                    boostBps = session.boostBps,
                    partySize = session.partySize,
                    track = track.map { it.toDto() },
                ),
            )

            when (result) {
                is ClaimResult.Signed -> {
                    sessionDao.update(
                        session.copy(
                            uploadState = UploadState.SIGNED.name,
                            uploadAttemptedAt = now(),
                            uploadAttempts = session.uploadAttempts + 1,
                            uploadError = "",
                            verdict = result.verdict,
                            claimSignature = result.signature,
                            claimSessionHash = result.claim.sessionHash,
                            claimAmount = result.claim.amount,
                            claimDay = result.claim.day,
                            claimDeadline = result.claim.deadline,
                        ),
                    )
                    signed++
                }

                is ClaimResult.Rejected -> {
                    sessionDao.update(
                        session.rejected(result.reason).copy(verdict = result.verdict.orEmpty()),
                    )
                    rejected++
                }

                is ClaimResult.Retry -> {
                    sessionDao.update(
                        session.copy(
                            uploadState = UploadState.FAILED.name,
                            uploadAttemptedAt = now(),
                            uploadAttempts = session.uploadAttempts + 1,
                            uploadError = result.reason,
                        ),
                    )
                    failed++
                    // 한 번 끊기면 다음 것도 끊긴다. 남은 세션까지 줄줄이
                    // 실패시켜 시도 횟수만 올릴 이유가 없다.
                    break
                }
            }
        }

        return UploadRun(
            signed = signed,
            rejected = rejected,
            failed = failed,
            shouldRetry = failed > 0,
        )
    }

    private fun WalkSessionEntity.rejected(reason: String) = copy(
        uploadState = UploadState.REJECTED.name,
        uploadAttemptedAt = now(),
        uploadAttempts = uploadAttempts + 1,
        uploadError = reason,
    )
}
