package com.example.presentation.screens.splash

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.R
import com.example.presentation.navigation.Routes

/**
 * Production-ready splash screen.
 *
 * Flow:
 * 1. Renders immediately with logo scale-in animation.
 * 2. Delegates all startup timing to [SplashViewModel].
 * 3. Navigates via [LaunchedEffect] when [SplashUiState.destinationRoute] is set.
 *
 * Guarantees no blank screen, no flickering, no navigation before both
 * app initialization AND minimum 2-second splash duration are satisfied.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

    // ── Start initialization + timing on first composition ──────────
    LaunchedEffect(Unit) {
        viewModel.onSplashStarted()
    }

    // ── Logo scale-in animation ────────────────────────────────────
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    // ── Navigate when ViewModel signals readiness ──────────────────
    LaunchedEffect(uiState.destinationRoute) {
        val route = uiState.destinationRoute ?: return@LaunchedEffect
        // Only trigger navigation if we haven't already navigated in this Activity session.
        // This prevents hijacking the navigation state during Activity recreation
        // if the NavController has already restored a different screen to the top.
        if (!hasNavigated) {
            hasNavigated = true
            // Short fade transition before navigating
            kotlinx.coroutines.delay(200)
            navController.navigate(route) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    // ── Progress bar visibility (fades in after logo animation) ────
    val showProgress by remember {
        derivedStateOf { scale.value > 0.5f }
    }

    // ── UI ─────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.habit_icon_logo),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(id = R.string.splash_sub),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )

            val context = LocalContext.current
            val quote = remember {
                com.example.util.MotivationalQuoteProvider(context).getRandomQuote()
            }
            Text(
                text = "\"$quote\"",
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp, start = 32.dp, end = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Glassmorphism loading indicator
            if (showProgress) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp) // slightly thicker
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.splash_loading),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
