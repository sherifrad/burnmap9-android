package com.drsherif.ruleofnines.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

internal fun percent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

@Composable
fun ResultsPanel(viewModel: BurnViewModel, modifier: Modifier = Modifier, compact: Boolean = false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Card(
        modifier = modifier.testTag("resultsPanel"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
            if (!compact || state.totalTbsaPercent == null) {
                Text("Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            when {
                state.isAnalyzing -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Analyzing both views…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                state.error != null -> Text(
                    "Analysis unavailable: ${state.error}",
                    Modifier.testTag("analysisError").semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                )
                state.isOutdated -> Text(
                    "Drawing changed. Analyze again for an updated estimate.",
                    Modifier.testTag("outdatedResult").semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.totalTbsaPercent != null -> {
                    if (compact) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total TBSA", style = MaterialTheme.typography.titleMedium)
                            Text(percent(state.totalTbsaPercent!!), Modifier.testTag("totalTbsa")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("TOTAL BURNED SURFACE", style = MaterialTheme.typography.labelSmall)
                            Text(
                                percent(state.totalTbsaPercent!!),
                                Modifier.testTag("totalTbsa").semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text("TBSA", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth()) {
                        Text("REGION", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                        Text("COVERAGE", Modifier.width(72.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
                        Text("TBSA", Modifier.width(72.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
                    }
                    LazyColumn(Modifier.weight(1f).testTag("regionResults")) {
                        items(state.results, key = { it.region }) { result ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = if (compact) 6.dp else 10.dp)
                                    .testTag("region_${result.region.name}").semantics(mergeDescendants = true) {},
                            ) {
                                Text(result.region.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(percent(result.coverageFraction * 100), Modifier.width(72.dp),
                                    textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                                Text(percent(result.tbsaPercent), Modifier.width(72.dp),
                                    textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                else -> Text(
                    "Paint the burned areas, then analyze. Both front and back are included.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
