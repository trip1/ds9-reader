package com.example.ds9reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.ds9reader.domain.CalibreConfig
import io.ktor.util.encodeBase64

@Composable
fun CoverImage(
    coverUrl: String?,
    config: CalibreConfig,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 84.dp,
) {
    val context = LocalPlatformContext.current
    val request = remember(coverUrl, config.username, config.password) {
        val builder = ImageRequest.Builder(context)
            .data(coverUrl?.takeIf { it.isNotBlank() })
            .crossfade(true)
        if (!coverUrl.isNullOrBlank() && config.username.isNotBlank()) {
            val token = "${config.username}:${config.password}".encodeBase64()
            val headers = NetworkHeaders.Builder()
                .set("Authorization", "Basic $token")
                .build()
            builder.httpHeaders(headers)
        }
        builder.build()
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = request,
                contentDescription = "Book cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
