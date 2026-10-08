package com.drsherif.ruleofnines.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drsherif.ruleofnines.model.BodyView
import com.drsherif.ruleofnines.R

@Composable
fun BurnScreen(viewModel: BurnViewModel = viewModel()) {
    var selectedView by rememberSaveable { mutableStateOf(BodyView.FRONT) }
    var brushWidth by rememberSaveable { mutableFloatStateOf(24f) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            if (landscape) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            val selectView: (BodyView) -> Unit = { viewModel.paintController.end(); selectedView = it }
            val changeBrush: (Float) -> Unit = { viewModel.paintController.end(); brushWidth = it }
            if (landscape) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DrawingPanel(viewModel, selectedView, selectView, brushWidth, changeBrush,
                        showControls = false, modifier = Modifier.weight(1f).fillMaxSize())
                    Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ResultsPanel(viewModel, Modifier.weight(1f).fillMaxWidth(), compact = true)
                        BrushControl(brushWidth, changeBrush)
                        AnalysisButtons(viewModel)
                    }
                }
            } else {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DrawingPanel(viewModel, selectedView, selectView, brushWidth, changeBrush,
                        showControls = true, modifier = Modifier.weight(0.62f).fillMaxWidth())
                    ResultsPanel(viewModel, Modifier.weight(0.38f).fillMaxWidth())
                }
            }
            Text(
                "Partial/full-thickness burns only; exclude superficial erythema. Schematic estimate—clinical review required.",
                Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DrawingPanel(
    viewModel: BurnViewModel,
    selectedView: BodyView,
    onSelectedViewChange: (BodyView) -> Unit,
    brushWidth: Float,
    onBrushWidthChange: (Float) -> Unit,
    showControls: Boolean,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            BodyView.entries.forEachIndexed { index, view ->
                SegmentedButton(
                    selected = selectedView == view,
                    onClick = { onSelectedViewChange(view) },
                    shape = SegmentedButtonDefaults.itemShape(index, BodyView.entries.size),
                    modifier = Modifier.testTag("view_${view.name}"),
                ) { Text(view.title) }
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (selectedView == BodyView.FRONT) "Patient RIGHT" else "Patient LEFT",
                    style = MaterialTheme.typography.labelSmall)
                Text(if (selectedView == BodyView.FRONT) "Patient LEFT" else "Patient RIGHT",
                    style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.weight(1f).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))) {
                BodyPaintCanvas(viewModel.diagrams.getValue(selectedView), viewModel.paintController,
                    brushWidth, Modifier.fillMaxSize())
            }
        }
        if (showControls) {
            BrushControl(brushWidth, onBrushWidthChange)
            AnalysisButtons(viewModel)
        }
    }
}

@Composable
private fun BrushControl(brushWidth: Float, onValueChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Brush", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = brushWidth,
            onValueChange = onValueChange,
            valueRange = 8f..64f,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Brush size" },
        )
    }
}

@Composable
private fun AnalysisButtons(viewModel: BurnViewModel) {
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { viewModel.paintController.end(); viewModel.analyze() },
            enabled = !controls.isAnalyzing,
            modifier = Modifier.weight(1f).testTag("analyzeButton"),
        ) { Text(if (controls.isAnalyzing) "Analyzing…" else "Analyze both views") }
        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.testTag("clearButton")) {
            Text("Clear all")
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear both drawings?") },
            text = { Text("This removes all paint from the front and back and resets the results.") },
            confirmButton = { TextButton(onClick = {
                viewModel.clearAll(); confirmClear = false
            }, modifier = Modifier.testTag("confirmClear")) { Text("Clear drawings") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
