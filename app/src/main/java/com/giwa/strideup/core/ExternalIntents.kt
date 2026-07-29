package com.giwa.strideup.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 앱 밖으로 나가는 인텐트들.
 *
 * 지도는 구글 지도를 먼저 노리고, 없으면 다른 지도 앱, 그것도 없으면
 * 브라우저로 구글 지도 웹을 연다. 어느 단계에서 실패해도 앱이 죽지 않는다.
 */
object ExternalIntents {

    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    /** 장소 이름으로 구글 지도를 연다. 이름이 비어 있으면 아무것도 하지 않는다. */
    fun openPlaceInMaps(context: Context, place: String) {
        val query = place.trim()
        if (query.isEmpty()) return
        val encoded = Uri.encode(query)

        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
        // 구글 지도가 깔려 있으면 바로 그쪽으로
        if (start(context, Intent(geo).setPackage(GOOGLE_MAPS))) return
        // 아니면 지도 인텐트를 받을 수 있는 아무 앱
        if (start(context, geo)) return
        // 마지막 수단 — 브라우저
        start(
            context,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded"),
            ),
        )
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
