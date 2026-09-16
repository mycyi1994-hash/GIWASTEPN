package com.stepup.android.data.remote

import com.stepup.android.domain.TrackPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 러닝 증명 서버(`attester/`)와 주고받는 형식.
 *
 * 이 파일의 필드 이름은 서버가 읽는 이름과 **글자 그대로 같아야 한다.**
 * 서버 쪽 규격은 `attester/src/index.js` 의 `/claim` 처리부에 있고, 한쪽만
 * 고치면 요청이 400으로 거절되거나 — 더 나쁘게는 — 기본값으로 조용히
 * 처리된다. 바꿀 때는 양쪽을 같이 본다.
 */
@Serializable
data class ClaimRequest(
    /** 보상을 받을 지갑 주소. 지갑이 붙기 전에는 이 값이 없어 업로드하지 않는다. */
    val runner: String,
    /** 세션 시작 시각 (epoch 밀리초) */
    val startedAt: Long,
    /** 세션 종료 시각 (epoch 밀리초) */
    val endedAt: Long,
    val steps: Int,
    /** 스니커즈 부스트. 1780 = +17.8% */
    val boostBps: Int,
    /** 함께 뛴 인원 (혼자면 1) */
    val partySize: Int,
    /** 시각이 붙은 GPS 경로. 서버가 이걸로 구간 속도를 다시 계산한다. */
    val track: List<TrackPointDto>,
)

/**
 * 좌표 하나.
 *
 * 시각 필드 이름이 `t` 인 것은 서버가 그렇게 읽기 때문이다
 * (`attester/src/economy.js` 의 `inspectTrack`). 앱 안에서는 [TrackPoint.at] 로
 * 부르지만 전송할 때는 서버 이름을 따른다.
 */
@Serializable
data class TrackPointDto(
    val lat: Double,
    val lng: Double,
    @SerialName("t") val at: Long,
)

fun TrackPoint.toDto(): TrackPointDto = TrackPointDto(lat = lat, lng = lng, at = at)

/**
 * 서버 응답.
 *
 * 성공하면 서명된 청구서가 온다. 이 서명이 있어야 `RewardDistributor` 가
 * 지급을 받아들인다 — 앱이 직접 만들 수 없는, 이 왕복의 결과물이다.
 *
 * 실패해도 같은 모양으로 오고 [ok] 가 false다. 서버는 판정에 실패한 이유를
 * [error] 에 사람이 읽을 수 있게 적어 보낸다.
 */
@Serializable
data class ClaimResponse(
    val ok: Boolean,
    /** CLEAN · FLAGGED · VOID — 서버가 내린 판정 */
    val verdict: String? = null,
    val claim: SignedClaim? = null,
    val signature: String? = null,
    val error: String? = null,
    /** 서버가 다시 계산한 경로 분석 — 왜 그렇게 판정했는지의 근거 */
    val inspection: Inspection? = null,
)

@Serializable
data class SignedClaim(
    val runner: String,
    val sessionHash: String,
    /** SUP 지급액(wei 단위 문자열). 18자리라 Long 으로는 담기지 않는다. */
    val amount: String,
    /** 배출 스케줄 기준 일자 */
    val day: Long,
    /** 이 서명이 유효한 마지막 시각 (epoch 초) */
    val deadline: Long,
)

@Serializable
data class Inspection(
    val validSegments: Int = 0,
    val flaggedSegments: Int = 0,
    val validMeters: Double = 0.0,
    val topSpeedKmh: Double = 0.0,
)
