package com.aven.app

import com.aven.app.core.calc.CompareResult
import com.aven.app.core.calc.Confidence
import com.aven.app.core.calc.ExamSnap
import com.aven.app.core.calc.Progress
import com.aven.app.core.calc.ScoreEngine
import com.aven.app.core.calc.SubjScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEngineTest {

    private fun exam(
        id: Long,
        title: String,
        daysAgo: Long,
        type: String = "月考",
        stage: String = "高二",
        vararg subjects: Triple<String, Double, Double>,
    ) = ExamSnap(
        id = id,
        title = title,
        date = 1_700_000_000_000L - daysAgo * 86_400_000L,
        type = type,
        stage = stage,
        subjects = subjects.map { SubjScore(it.first, it.second, it.third) },
    )

    @Test
    fun improvementWithSameFullScore() {
        val prev = exam(1, "十月月考", 30, subjects = arrayOf(
            Triple("语文", 105.0, 150.0),
            Triple("数学", 110.0, 150.0),
            Triple("英语", 118.0, 150.0),
        ))
        val last = exam(2, "期中考试", 1, type = "月考", subjects = arrayOf(
            Triple("语文", 108.0, 150.0),
            Triple("数学", 120.0, 150.0),
            Triple("英语", 121.0, 150.0),
        ))
        val r = ScoreEngine.compare(listOf(last, prev), 2.0)
        assertEquals(Progress.IMPROVED, r.state)
        assertEquals(16.0, r.deltaRaw ?: 0.0, 0.01)
        assertTrue(r.deltaRatePp > 2.0)
        assertEquals(Confidence.HIGH, r.confidence)
        assertEquals(3, r.commonSubjects.size)
    }

    @Test
    fun differentFullScoreUsesRateNotRaw() {
        // 原始分下降 20 分，但得分率从 86.7% 提升到 91.7%
        val prev = exam(1, "十月月考", 30, subjects = arrayOf(Triple("数学", 130.0, 150.0)))
        val last = exam(2, "十一月月考", 1, subjects = arrayOf(Triple("数学", 110.0, 120.0)))
        val r = ScoreEngine.compare(listOf(last, prev), 2.0)
        assertEquals(Progress.IMPROVED, r.state)
        assertNull(r.deltaRaw)
        assertTrue(r.deltaRatePp > 4.0)
        assertTrue(r.confidence != Confidence.HIGH)
    }

    @Test
    fun smallChangeIsFlat() {
        val prev = exam(1, "十月月考", 30, subjects = arrayOf(Triple("物理", 80.0, 100.0)))
        val last = exam(2, "十一月月考", 1, subjects = arrayOf(Triple("物理", 81.0, 100.0)))
        val r = ScoreEngine.compare(listOf(last, prev), 2.0)
        assertEquals(Progress.FLAT, r.state)
    }

    @Test
    fun noCommonSubjectIsInsufficient() {
        val prev = exam(1, "十月月考", 30, subjects = arrayOf(Triple("历史", 80.0, 100.0)))
        val last = exam(2, "十一月月考", 1, subjects = arrayOf(Triple("化学", 80.0, 100.0)))
        val r = ScoreEngine.compare(listOf(last, prev), 2.0)
        assertEquals(Progress.INSUFFICIENT, r.state)
        assertTrue(r.reasons.isNotEmpty())
    }

    @Test
    fun singleExamIsInsufficient() {
        val only = exam(1, "第一次月考", 1, subjects = arrayOf(Triple("语文", 100.0, 150.0)))
        val r = ScoreEngine.compare(listOf(only), 2.0)
        assertEquals(Progress.INSUFFICIENT, r.state)
    }

    @Test
    fun preferSameExamType() {
        val oldSameType = exam(1, "九月月考", 60, type = "月考", subjects = arrayOf(Triple("数学", 100.0, 150.0)))
        val recentOther = exam(2, "秋季运动会", 10, type = "其他", subjects = arrayOf(Triple("数学", 140.0, 150.0)))
        val last = exam(3, "十月月考", 1, type = "月考", subjects = arrayOf(Triple("数学", 120.0, 150.0)))
        val r = ScoreEngine.compare(listOf(last, recentOther, oldSameType), 2.0, rule = 0)
        assertEquals(oldSameType.id, r.prevId)
        assertEquals(Progress.IMPROVED, r.state)
    }

    @Test
    fun bestCombinationAcrossDifferentFullScores() {
        val a = exam(1, "上学期期末", 200, subjects = arrayOf(
            Triple("语文", 130.0, 150.0),
            Triple("数学", 95.0, 150.0),
        ))
        val b = exam(2, "本次月考", 1, subjects = arrayOf(
            Triple("语文", 110.0, 150.0),
            Triple("数学", 90.0, 150.0),
        ))
        val combo = ScoreEngine.bestCombination(b, listOf(a, b))
        assertEquals(130.0 + 95.0, combo.bestTotal, 0.01)
        assertEquals(25.0, combo.gapTotal, 0.01)
        assertEquals("语文", combo.rows.first().subject)
    }

    @Test
    fun subjectBestsTrackRawAndRateSeparately() {
        val a = exam(1, "满分150", 100, subjects = arrayOf(Triple("数学", 135.0, 150.0))) // 90%
        val b = exam(2, "满分100", 10, subjects = arrayOf(Triple("数学", 92.0, 100.0)))   // 92%
        val best = ScoreEngine.subjectBests(listOf(a, b)).first()
        assertEquals(135.0, best.bestRaw, 0.01)
        assertEquals(0.92, best.bestRate, 0.001)
        assertEquals(2, best.count)
    }

    @Test
    fun goalProgressClamps() {
        assertEquals(1f, ScoreEngine.goalProgress(620.0, 600.0), 0.001f)
        assertEquals(0.5f, ScoreEngine.goalProgress(300.0, 600.0), 0.001f)
        assertEquals(0f, ScoreEngine.goalProgress(null, 600.0), 0.001f)
    }
}
