package com.drsherif.ruleofnines.model

enum class BodyView(val title: String) { FRONT("Front"), BACK("Back") }

enum class BodyRegion(val title: String) {
    HEAD_NECK("Head & neck"), FRONT_TRUNK("Front trunk"), BACK_TRUNK("Back trunk"),
    RIGHT_ARM("Right arm"), LEFT_ARM("Left arm"), RIGHT_LEG("Right leg"),
    LEFT_LEG("Left leg"), PERINEUM("Perineum")
}

enum class BodySurface(
    val view: BodyView,
    val region: BodyRegion,
    val weightPercent: Double,
    val argb: Int,
) {
    FRONT_HEAD(BodyView.FRONT, BodyRegion.HEAD_NECK, 4.5, 0xFF0000FF.toInt()),
    BACK_HEAD(BodyView.BACK, BodyRegion.HEAD_NECK, 4.5, 0xFF000080.toInt()),
    FRONT_TRUNK(BodyView.FRONT, BodyRegion.FRONT_TRUNK, 18.0, 0xFFFFFF00.toInt()),
    BACK_TRUNK(BodyView.BACK, BodyRegion.BACK_TRUNK, 18.0, 0xFF808000.toInt()),
    FRONT_RIGHT_ARM(BodyView.FRONT, BodyRegion.RIGHT_ARM, 4.5, 0xFF00FF00.toInt()),
    BACK_RIGHT_ARM(BodyView.BACK, BodyRegion.RIGHT_ARM, 4.5, 0xFF008000.toInt()),
    FRONT_LEFT_ARM(BodyView.FRONT, BodyRegion.LEFT_ARM, 4.5, 0xFFFF00FF.toInt()),
    BACK_LEFT_ARM(BodyView.BACK, BodyRegion.LEFT_ARM, 4.5, 0xFF800080.toInt()),
    FRONT_RIGHT_LEG(BodyView.FRONT, BodyRegion.RIGHT_LEG, 9.0, 0xFF00FFFF.toInt()),
    BACK_RIGHT_LEG(BodyView.BACK, BodyRegion.RIGHT_LEG, 9.0, 0xFF008080.toInt()),
    FRONT_LEFT_LEG(BodyView.FRONT, BodyRegion.LEFT_LEG, 9.0, 0xFFFF8000.toInt()),
    BACK_LEFT_LEG(BodyView.BACK, BodyRegion.LEFT_LEG, 9.0, 0xFF804000.toInt()),
    PERINEUM(BodyView.FRONT, BodyRegion.PERINEUM, 1.0, 0xFFFFFFFF.toInt());

    companion object {
        fun forView(view: BodyView): List<BodySurface> = entries.filter { it.view == view }
        fun palette(view: BodyView): Map<Int, BodySurface> = forView(view).associateBy { it.argb }
    }
}

data class SurfaceCoverage(
    val surface: BodySurface,
    val totalPixels: Int,
    val paintedPixels: Int,
) {
    init {
        require(totalPixels > 0) { "Region has no pixels: $surface" }
        require(paintedPixels in 0..totalPixels) { "Invalid painted pixel count: $surface" }
    }
    val fraction: Double get() = paintedPixels.toDouble() / totalPixels
}

data class RegionResult(
    val region: BodyRegion,
    val coverageFraction: Double,
    val tbsaPercent: Double,
)

data class BurnUiState(
    val isAnalyzing: Boolean = false,
    val results: List<RegionResult> = emptyList(),
    val totalTbsaPercent: Double? = null,
    val error: String? = null,
    val isOutdated: Boolean = false,
    val analyzedRevision: Long? = null,
)
