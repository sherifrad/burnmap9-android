package com.drsherif.ruleofnines.analysis

import com.drsherif.ruleofnines.model.BodyRegion
import com.drsherif.ruleofnines.model.BodySurface
import com.drsherif.ruleofnines.model.RegionResult
import com.drsherif.ruleofnines.model.SurfaceCoverage

class RuleOfNinesCalculator {
    fun calculate(coverage: List<SurfaceCoverage>): List<RegionResult> {
        require(coverage.size == BodySurface.entries.size &&
            coverage.map { it.surface }.toSet() == BodySurface.entries.toSet()) {
            "A complete, unique set of front and back surfaces is required"
        }
        return BodyRegion.entries.map { region ->
            val surfaces = coverage.filter { it.surface.region == region }
            val weight = surfaces.sumOf { it.surface.weightPercent }
            val contribution = surfaces.sumOf { it.fraction * it.surface.weightPercent }
            RegionResult(region, contribution / weight, contribution)
        }
    }
}
