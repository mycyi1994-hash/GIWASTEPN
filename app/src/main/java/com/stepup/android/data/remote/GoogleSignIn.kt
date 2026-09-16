package com.stepup.android.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

/** 구글이 내준 신원 증명 */
sealed interface GoogleIdResult {
    /**
     * @property idToken 서버가 검증할 토큰
     * @property nonce   이 요청에 쓴 일회용 값. 서버도 같은 값을 확인한다.
     */
    data class Ok(val idToken: String, val nonce: String) : GoogleIdResult

    /** 사용자가 창을 닫았다. 오류가 아니므로 아무 말도 하지 않는다. */
    data object Cancelled : GoogleIdResult

    /** 이 기기에서 쓸 수 있는 구글 계정이 없다. */
    data class NoAccount(val reason: String) : GoogleIdResult

    data class Failed(val reason: String) : GoogleIdResult
}

/**
 * 안드로이드에서 구글 계정을 받아 온다.
 *
 * 브라우저를 띄우지 않는다. 시스템이 이미 아는 계정을 보여주고 한 번 탭하면
 * 끝난다 — 비밀번호도 이메일 입력도 없다. 로그인을 필수로 하면서도 마찰이
 * 크지 않은 이유다.
 *
 * **여기서 쓰는 것은 웹 클라이언트 ID 다.** 안드로이드 클라이언트 ID 가
 * 아니다. 안드로이드 쪽은 "이 앱이 진짜 맞다"를 구글이 확인하는 용도로
 * 등록만 해 두고 코드에는 넣지 않는다. 받는 토큰의 수신자(aud)는 웹
 * 클라이언트 ID 이고, 서버는 그 값으로 토큰을 검증한다.
 */
class GoogleSignIn(
    private val webClientId: String,
    private val credentialManager: (Context) -> CredentialManager = { CredentialManager.create(it) },
) {

    val isConfigured: Boolean get() = webClientId.isNotBlank()

    /**
     * @param activityContext 반드시 화면(Activity) 컨텍스트여야 한다. 앱
     *   컨텍스트를 넘기면 계정 선택 창을 띄울 곳이 없어 실패한다.
     * @param filterByAuthorized 이미 이 앱에 로그인한 적 있는 계정만 보여줄지.
     *   처음에는 true 로 물어보고, 없으면 false 로 다시 물어본다 — 그래야
     *   "계정이 있는데 아무것도 안 뜨는" 일이 없다.
     */
    suspend fun requestIdToken(
        activityContext: Context,
        filterByAuthorized: Boolean = true,
    ): GoogleIdResult {
        if (!isConfigured) return GoogleIdResult.Failed("구글 로그인이 설정되지 않았습니다")

        // 일회용 값. 받은 토큰이 **이 요청에 대한 답**임을 확인하는 데 쓴다.
        // 없으면 다른 데서 가로챈 토큰을 그대로 들이밀 수 있다.
        val nonce = newNonce()

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterByAuthorized)
            .setNonce(nonce.hashed)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = credentialManager(activityContext).getCredential(activityContext, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                GoogleIdResult.Ok(token, nonce.raw)
            } else {
                GoogleIdResult.Failed("예상하지 못한 자격 형식입니다")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleIdResult.Cancelled
        } catch (e: NoCredentialException) {
            // 이 기기에 쓸 수 있는 계정이 없다. 걸러 보고 있었다면 한 번 더
            // 넓혀서 물어본다 — 계정은 있는데 이 앱으로 로그인한 적이 없는
            // 경우가 첫 로그인에서는 당연하다.
            if (filterByAuthorized) {
                requestIdToken(activityContext, filterByAuthorized = false)
            } else {
                GoogleIdResult.NoAccount("기기에 구글 계정이 없습니다")
            }
        } catch (e: GetCredentialException) {
            GoogleIdResult.Failed(e.message ?: "구글 로그인에 실패했습니다")
        } catch (e: Exception) {
            GoogleIdResult.Failed(e.message ?: "구글 로그인에 실패했습니다")
        }
    }

    /**
     * 일회용 값 한 쌍.
     *
     * 구글에는 **해시**를 주고, 서버에는 **원본**을 준다. 서버가 원본을 해시해
     * 토큰 안의 값과 맞춰 보면, 그 토큰이 이 요청에 대한 답인지 확인된다.
     */
    private data class Nonce(val raw: String, val hashed: String)

    private fun newNonce(): Nonce {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val raw = bytes.toHex()
        val hashed = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .toHex()
        return Nonce(raw, hashed)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
