package com.stepup.android.data.repo

import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionRecorded
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
 * 세션 하나를 서버에 기록하는 쪽.
 *
 * 인터페이스로 둔 것은 업로드 규칙(무엇을 언제 다시 시도할지)을 실제 네트워크
 * 없이 검증하기 위해서다. 그 규칙이 틀리면 사용자가 정당하게 뛴 기록을 잃거나
 * 영원히 거절당할 요청으로 배터리를 태운다.
 */
interface SessionRecorder {
    val isConfigured: Boolean

    suspend fun record(session: WalkSessionEntity): ServerResult<SessionRecorded>
}

/**
 * 쌓인 러닝 세션을 서버로 올린다.
 *
 * 러닝이 끝나는 곳은 지하철이거나 산이다. 그 자리에서 바로 보내는 것을
 * 전제로 만들면 기록이 사라진다. 그래서 세션은 일단 기기에 쌓이고, 이
 * 저장소가 연결이 될 때 대신 밀어 올린다.
 *
 * 적립액은 서버가 정한다. 앱이 계산한 금액은 보내지도 않는다 —
 * `supabase/migrations/0003_ledger.sql` 의 `record_session()` 이 걸음 수를
 * 받아 직접 계산하고, 그 값을 돌려준다.
 */
class ClaimRepository(
    private val sessionDao: WalkSessionDao,
    private val recorder: SessionRecorder,
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
        if (!recorder.isConfigured) {
            return UploadRun(blockedBy = "서버 주소가 아직 설정되지 않았습니다")
        }

        val pending = sessionDao.pendingUploads(limit)
        if (pending.isEmpty()) return UploadRun()

        var signed = 0
        var rejected = 0
        var failed = 0

        for (session in pending) {
            when (val result = recorder.record(session)) {
                is ServerResult.Ok -> {
                    sessionDao.update(
                        session.copy(
                            uploadState = UploadState.SIGNED.name,
                            uploadAttemptedAt = now(),
                            uploadAttempts = session.uploadAttempts + 1,
                            uploadError = "",
                            verdict = result.value.verdict,
                            // 서버가 계산한 금액으로 덮는다. 앱이 화면에 보여준
                            // 값과 다를 수 있고, 다르면 서버 쪽이 맞다.
                            claimAmount = result.value.pointsAwarded.toString(),
                            claimDay = result.value.sessionId,
                        ),
                    )
                    signed++
                }

                is ServerResult.Rejected -> {
                    sessionDao.update(session.rejected(result.reason))
                    rejected++
                }

                is ServerResult.Retry -> {
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

                is ServerResult.SignInRequired -> {
                    // 다시 로그인해야 한다. 여기서 세션을 실패로 찍어 봐야
                    // 시도 횟수만 오른다 — 대기열을 그대로 두고 물러난다.
                    return UploadRun(
                        signed = signed,
                        rejected = rejected,
                        failed = failed,
                        blockedBy = result.reason,
                    )
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
