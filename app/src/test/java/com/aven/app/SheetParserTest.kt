package com.aven.app

import com.aven.app.core.ocr.SheetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetParserTest {

    private val sample = """
        汉台中学 2026 秋季学期
        高二年级期中考试成绩单
        姓名：明青柯  班级：高二(7)班
        考试日期：2026年11月5日
        语文 112 / 150
        数学 128/150
        英语 96 150
        物理 78
        化学 82 /100
        生物 90 / 100
        政治 76 / 100 排名 12/520
    """.trimIndent()

    @Test
    fun parsesSubjectsAndScores() {
        val r = SheetParser.parse(sample)
        assertEquals(7, r.subjects.size)
        val chinese = r.subjects.first { it.subject == "语文" }
        assertEquals(112.0, chinese.score!!, 0.01)
        assertEquals(150.0, chinese.full!!, 0.01)
        assertTrue(chinese.confident)

        val physics = r.subjects.first { it.subject == "物理" }
        assertEquals(78.0, physics.score!!, 0.01)
        assertEquals(100.0, physics.full!!, 0.01)
        // 只有一个数字时满分是推断的，不能标记为确定
        assertTrue(!physics.confident)
    }

    @Test
    fun parsesTitleAndDate() {
        val r = SheetParser.parse(sample)
        assertNotNull(r.title)
        assertTrue(r.title!!.contains("期中"))
        assertNotNull(r.dateMillis)
    }

    @Test
    fun parsesRankAndTotal() {
        val r = SheetParser.parse(sample)
        val politics = r.subjects.first { it.subject == "政治" }
        assertEquals(12, politics.rank)
        assertEquals(520, politics.total)
    }

    @Test
    fun handlesFullScoreFirstAndDecimal() {
        val r = SheetParser.parse("期末考试成绩\n数学 150 112.5\n英语 满分150 得分 101")
        val math = r.subjects.first { it.subject == "数学" }
        assertEquals(112.5, math.score!!, 0.01)
        assertEquals(150.0, math.full!!, 0.01)
        val english = r.subjects.first { it.subject == "英语" }
        assertEquals(101.0, english.score!!, 0.01)
        assertEquals(150.0, english.full!!, 0.01)
    }

    @Test
    fun ignoresLinesWithoutSubject() {
        val r = SheetParser.parse("总分 580\n班级平均 512\n准考证号 202611050012\n语文 110/150")
        assertEquals(1, r.subjects.size)
    }

    @Test
    fun emptyTextGivesEmptySheet() {
        val r = SheetParser.parse("")
        assertTrue(r.subjects.isEmpty())
    }
}
