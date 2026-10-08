package com.drsherif.ruleofnines.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drsherif.ruleofnines.analysis.PixelCoverageAnalyzer
import com.drsherif.ruleofnines.analysis.RuleOfNinesCalculator
import com.drsherif.ruleofnines.graphics.BodyDiagramFactory
import com.drsherif.ruleofnines.graphics.PaintController
import com.drsherif.ruleofnines.model.BodyView
import com.drsherif.ruleofnines.model.BurnUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnalysisControls(val isAnalyzing: Boolean = false)

class BurnViewModel(
    private val analyzer: PixelCoverageAnalyzer = PixelCoverageAnalyzer(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(BurnUiState())
    val state = _state.asStateFlow()
    val controls = state.map { AnalysisControls(it.isAnalyzing) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AnalysisControls())
    private var analysisJob: Job? = null
    val diagrams = BodyView.entries.associateWith { BodyDiagramFactory.create(it) }
    val paintController = PaintController { onPaintChanged() }

    private fun onPaintChanged() {
        analysisJob?.cancel()
        _state.value = _state.value.copy(
            isAnalyzing = false,
            isOutdated = true,
            totalTbsaPercent = null,
            error = null,
        )
    }

    fun analyze() {
        if (_state.value.isAnalyzing) return
        analysisJob?.cancel()
        val snapshot = try {
            paintController.snapshot()
        } catch (error: Exception) {
            _state.value = _state.value.copy(error = error.message ?: "Unable to snapshot drawing", totalTbsaPercent = null)
            return
        }
        _state.value = _state.value.copy(isAnalyzing = true, error = null, totalTbsaPercent = null)
        analysisJob = viewModelScope.launch {
            try {
                val results = withContext(dispatcher) {
                    val coverage = BodyView.entries.flatMap { view ->
                        val diagram = diagrams.getValue(view)
                        val masked = PixelCoverageAnalyzer.maskedPaint(snapshot.bitmaps.getValue(view), diagram.baseBitmap)
                        try {
                            analyzer.analyze(diagram.hiddenMap, masked, diagram.palette)
                        } finally {
                            masked.recycle()
                        }
                    }
                    RuleOfNinesCalculator().calculate(coverage)
                }
                if (paintController.revision == snapshot.revision) {
                    _state.value = BurnUiState(
                        results = results,
                        totalTbsaPercent = results.sumOf { it.tbsaPercent },
                        analyzedRevision = snapshot.revision,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (paintController.revision == snapshot.revision) {
                    _state.value = _state.value.copy(
                        isAnalyzing = false, totalTbsaPercent = null,
                        error = error.message ?: "Analysis failed. Try again.",
                    )
                }
            } finally {
                snapshot.release()
            }
        }
    }

    fun clearAll() {
        paintController.clearAll()
        _state.value = BurnUiState()
    }
}
