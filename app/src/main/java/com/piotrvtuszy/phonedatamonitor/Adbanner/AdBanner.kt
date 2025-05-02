package com.piotrvtuszy.phonedatamonitor.adbanner

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(context: Context, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-4782464337210507/3379228377"  // Twój prawdziwy AdUnit
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
