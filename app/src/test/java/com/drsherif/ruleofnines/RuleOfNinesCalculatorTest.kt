package com.drsherif.ruleofnines

import com.drsherif.ruleofnines.analysis.RuleOfNinesCalculator
import com.drsherif.ruleofnines.model.BodyRegion
import com.drsherif.ruleofnines.model.BodySurface
import com.drsherif.ruleofnines.model.BodyView
import com.drsherif.ruleofnines.model.SurfaceCoverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleOfNinesCalculatorTest {
    private val calculator = RuleOfNinesCalculator()
    private fun coverage(painted: (BodySurface) -> Int) = BodySurface.entries.map { SurfaceCoverage(it, 100, painted(it)) }
    private fun total(painted: (BodySurface) -> Int) = calculator.calculate(coverage(painted)).sumOf { it.tbsaPercent }

    @Test fun approvedWeightsSumToOneHundred() {
        assertEquals(100.0, BodySurface.entries.sumOf { it.weightPercent }, 0.0)
        assertEquals(13, BodySurface.entries.size)
    }
    @Test fun emptyAndCompletePainting() {
        assertEquals(0.0, total { 0 }, 0.0)
        assertEquals(100.0, total { 100 }, 0.0)
    }
    @Test fun correctAnteriorAndPosteriorTotals() {
        assertEquals(50.5, total { if (it.view == BodyView.FRONT) 100 else 0 }, 0.0)
        assertEquals(49.5, total { if (it.view == BodyView.BACK) 100 else 0 }, 0.0)
    }
    @Test fun halfFrontRightArmIsTwoPointTwoFive() {
        val result = calculator.calculate(coverage { if (it == BodySurface.FRONT_RIGHT_ARM) 50 else 0 })
            .single { it.region == BodyRegion.RIGHT_ARM }
        assertEquals(2.25, result.tbsaPercent, 0.0)
        assertEquals(0.25, result.coverageFraction, 0.0)
    }
    @Test fun bothArmSurfacesAndBothWholeArms() {
        assertEquals(9.0, total { if (it.region == BodyRegion.RIGHT_ARM) 100 else 0 }, 0.0)
        assertEquals(18.0, total { if (it.region in setOf(BodyRegion.RIGHT_ARM, BodyRegion.LEFT_ARM)) 100 else 0 }, 0.0)
    }
    @Test fun perineumCountedExactlyOnce() {
        assertEquals(1, BodySurface.entries.count { it.region == BodyRegion.PERINEUM })
        assertEquals(1.0, total { if (it == BodySurface.PERINEUM) 100 else 0 }, 0.0)
    }
    @Test fun differingProjectionPixelCountsDoNotReweightAnatomy() {
        val input = coverage { 0 }.map {
            when (it.surface) {
                BodySurface.FRONT_RIGHT_ARM -> SurfaceCoverage(it.surface, 10000, 5000)
                BodySurface.BACK_RIGHT_ARM -> SurfaceCoverage(it.surface, 100, 100)
                else -> it
            }
        }
        val arm = calculator.calculate(input).single { it.region == BodyRegion.RIGHT_ARM }
        assertEquals(6.75, arm.tbsaPercent, 0.0)
        assertEquals(0.75, arm.coverageFraction, 0.0)
    }
    @Test fun noPrematureRounding() {
        val result = calculator.calculate(BodySurface.entries.map {
            SurfaceCoverage(it, 3, if (it == BodySurface.FRONT_HEAD) 1 else 0)
        })
        assertEquals(1.5, result.sumOf { it.tbsaPercent }, 0.0000001)
        assertTrue(result.all { it.coverageFraction.isFinite() && it.coverageFraction in 0.0..1.0 })
    }
    @Test(expected = IllegalArgumentException::class) fun missingSurfaceRejected() {
        calculator.calculate(coverage { 0 }.dropLast(1))
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateSurfaceRejected() {
        val input = coverage { 0 }
        calculator.calculate(input.dropLast(1) + input.first())
    }
    @Test(expected = IllegalArgumentException::class) fun zeroDenominatorRejected() {
        SurfaceCoverage(BodySurface.FRONT_HEAD, 0, 0)
    }
    @Test(expected = IllegalArgumentException::class) fun outOfRangeCoverageRejected() {
        SurfaceCoverage(BodySurface.FRONT_HEAD, 100, 101)
    }
}
