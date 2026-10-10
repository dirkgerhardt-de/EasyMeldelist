package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dirkgerhardt.easymeldelist.R
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color

/**
 * Branding-Seite direkt nach dem nativen Splash:
 * Logo + Claim "Melden · Mitmachen · Medaille!",
 * ca. 4 Sekunden, gestaffelter Fade-in (Logo zuerst, dann Text).
 */
@Composable
fun BrandingScreen(onFinished: () -> Unit) {

    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)      // kurze Anlaufzeit, dann Logo-Fade-in
        logoVisible = true
        delay(700)     // Logo kurz allein wirken lassen
        textVisible = true
        delay(3200)    // Logo + Claim gemeinsam sichtbar
        onFinished()   // Gesamt: ~4 Sekunden
    }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "logoAlpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "textAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF86C7EB)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "EasyMeldelist Logo",
            modifier = Modifier
                .size(160.dp)
                .alpha(logoAlpha)
        )
        Text(
            text = "Melden · Mitmachen · Medaille!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),  // ← HIER geändert: garantierter Kontrast auf #86C7EB
            modifier = Modifier
                .padding(top = 24.dp)
                .alpha(textAlpha)
        )
    }
}