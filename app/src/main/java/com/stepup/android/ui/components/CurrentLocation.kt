package com.stepup.android.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.stepup.android.domain.GeoPoint

/**
 * 지금 내가 있는 자리. 아직 모르면 null 이다.
 *
 * 러닝을 시작하기 전에는 위치 서비스가 돌지 않는다 — 그건 전경 서비스의 일이고,
 * 시작도 안 한 러닝이 배터리를 먹게 둘 수는 없다. 그런데 "코스를 안 골랐을 때
 * 내가 선 자리를 지도로 보여 준다"에는 좌표 하나면 된다.
 *
 * 그래서 두 단계로 받는다.
 *  1. 마지막으로 알려진 위치 — 즉시 나온다. 대개 몇 분 전 것이고, 지도를
 *     내 동네에 맞추는 데는 충분하다.
 *  2. 그다음 갱신 — 화면이 떠 있는 동안만 받는다. [enabled] 가 꺼지거나
 *     화면을 떠나면 바로 끊는다.
 *
 * 권한이 없거나 위치가 꺼져 있으면 조용히 null 을 낸다. 러닝 자체는 걸음
 * 센서로 되므로, 여기서 권한을 다시 조르지 않는다.
 *
 * @param enabled 끄면 갱신을 받지 않고 마지막 값도 지운다. 보여 줄 다른 것(코스·
 *   실시간 경로)이 생기면 꺼서, 안 보이는 지도를 위해 GPS 를 켜 두지 않는다.
 */
@Composable
fun rememberCurrentLocation(enabled: Boolean = true): GeoPoint? {
    val context = LocalContext.current
    var here by remember { mutableStateOf<GeoPoint?>(null) }

    DisposableEffect(enabled) {
        if (!enabled || !context.canReadLocation()) {
            here = null
            return@DisposableEffect onDispose { }
        }
        val manager = ContextCompat.getSystemService(context, LocationManager::class.java)
        if (manager == null) {
            return@DisposableEffect onDispose { }
        }

        here = manager.lastKnown() ?: here

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                here = GeoPoint(location.latitude, location.longitude)
            }
        }

        val provider = manager.bestProvider()
        if (provider != null) {
            runCatching {
                // 3초·5m — 지도를 내 자리에 맞추는 데 필요한 만큼이다.
                // 러닝 중의 경로 기록(2.5초·6m)과 달리 여기서는 정밀도가
                // 아니라 "대충 어디냐"만 있으면 된다.
                manager.requestLocationUpdates(provider, 3_000L, 5f, listener, Looper.getMainLooper())
            }
        }

        onDispose { runCatching { manager.removeUpdates(listener) } }
    }

    return here
}

private fun Context.canReadLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 마지막으로 알려진 위치 중 가장 최근 것.
 *
 * 공급자마다 따로 들고 있어서, GPS 만 물으면 실내에서 몇 시간 전 것이 나오고
 * 네트워크 쪽에 있는 1분 전 위치를 놓친다. 셋 다 물어 보고 시각으로 고른다.
 */
private fun LocationManager.lastKnown(): GeoPoint? = runCatching {
    listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
        .mapNotNull { provider -> runCatching { getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { GeoPoint(it.latitude, it.longitude) }
}.getOrNull()

/** 지금 쓸 수 있는 공급자. GPS 가 꺼져 있으면 네트워크로 내려간다. */
private fun LocationManager.bestProvider(): String? = runCatching {
    when {
        isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
}.getOrNull()
