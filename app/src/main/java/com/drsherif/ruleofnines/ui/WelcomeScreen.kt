package com.drsherif.ruleofnines.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drsherif.ruleofnines.R
import kotlinx.coroutines.delay

/** A welcome is shown once per saved activity session, never on resume or calculator rotation. */
@Composable
fun BurnMapApp() {
    var welcomeComplete by rememberSaveable { mutableStateOf(false) }
    if (welcomeComplete) {
        BurnScreen()
    } else {
        LaunchedEffect(Unit) {
            delay(1_200L)
            welcomeComplete = true
        }
        WelcomeScreen(onContinue = { welcomeComplete = true })
    }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val logo = remember(context) {
        // Reuse the actual launcher XML; Honor's normal drawable lookup can return
        // an OEM-themed bitmap wrapper instead of the underlying adaptive resource.
        val drawable = context.resources.getXml(context.applicationInfo.icon).use { parser ->
            Drawable.createFromXml(context.resources, parser, context.theme)
        }
        Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(Canvas(bitmap))
        }.asImageBitmap()
    }
    val continueLabel = stringResource(R.string.welcome_continue)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(colorResource(R.color.welcome_background))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("welcomeScreen")
            .clickable(role = Role.Button, onClickLabel = continueLabel, onClick = onContinue),
    ) {
        val logoSize = if (maxHeight < 400.dp) 112.dp else 160.dp
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                bitmap = logo,
                contentDescription = stringResource(R.string.welcome_logo_description),
                modifier = Modifier.size(logoSize).clip(RoundedCornerShape(28.dp)).testTag("welcomeLogo"),
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Text(
            stringResource(R.string.creator_credit),
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(vertical = 8.dp).testTag("creatorCredit"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
