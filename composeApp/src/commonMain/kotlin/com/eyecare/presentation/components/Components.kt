package com.eyecare.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.core.advertisement.AdBanner
import com.eyecare.core.advertisement.AdManager
import com.eyecare.core.advertisement.AdPlacement
import com.eyecare.presentation.Strings
import com.eyecare.resources.Res
import com.eyecare.resources.ad_banner_1
import com.eyecare.resources.ad_banner_2
import com.eyecare.resources.ad_banner_3
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private fun AdBanner.drawable(): DrawableResource = when (this) {
    AdBanner.BANNER_1 -> Res.drawable.ad_banner_1
    AdBanner.BANNER_2 -> Res.drawable.ad_banner_2
    AdBanner.BANNER_3 -> Res.drawable.ad_banner_3
}

/** Static banner (US-18). Renders nothing on placements where ads are not allowed. */
@Composable
fun AdBannerView(adManager: AdManager, placement: AdPlacement, modifier: Modifier = Modifier) {
    val banner = remember(placement) { adManager.bannerFor(placement) } ?: return
    Image(
        painter = painterResource(banner.drawable()),
        contentDescription = banner.description,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxWidth().aspectRatio(960f / 180f).clip(RoundedCornerShape(12.dp)),
    )
}

/** Common page frame: title row with optional back button, scrollable content, optional footer (ad). */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) {
                TextButton(onClick = onBack) { Text("‹ ${Strings.BACK}") }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp),
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        if (footer != null) {
            Column(Modifier.padding(top = 12.dp)) { footer() }
        }
    }
}
