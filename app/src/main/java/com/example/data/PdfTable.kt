package com.example.data

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream

/**
 * Writes a table as a PDF file (A4 landscape) with Android's own PdfDocument. Text is laid out by
 * StaticLayout, so Arabic letters join and run right-to-left; columns run from the right edge.
 * The header row is repeated on every page.
 */
object PdfTable {
    private const val PAGE_W = 842
    private const val PAGE_H = 595
    private const val MARGIN = 24f
    private const val PAD = 4f

    fun write(title: String, subtitle: String, header: List<String>, rows: List<List<Any?>>, weights: List<Int>): ByteArray {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(6, 78, 59) }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.rgb(71, 85, 105) }
        val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(6, 78, 59) }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.rgb(15, 23, 42) }
        val line = Paint().apply { color = Color.rgb(148, 163, 184); style = Paint.Style.STROKE; strokeWidth = 0.6f }
        val headFill = Paint().apply { color = Color.rgb(209, 250, 229); style = Paint.Style.FILL }

        val tableW = PAGE_W - 2 * MARGIN
        val total = weights.sum().toFloat()
        val widths = weights.map { tableW * it / total }

        fun layout(text: String, paint: TextPaint, width: Float): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, (width - 2 * PAD).toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

        fun layoutRow(cells: List<String>, paint: TextPaint) = cells.mapIndexed { i, t -> layout(t, paint, widths[i]) }
        fun heightOf(ls: List<StaticLayout>) = (ls.maxOfOrNull { it.height } ?: 0) + 2 * PAD

        val headLayouts = layoutRow(header, headPaint)
        val headH = heightOf(headLayouts)
        val bodyLayouts = rows.map { r -> layoutRow(header.indices.map { r.getOrNull(it)?.toString() ?: "" }, cellPaint) }

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun drawRow(ls: List<StaticLayout>, h: Float, fill: Paint?) {
            val canvas = page!!.canvas
            var right = PAGE_W - MARGIN
            ls.forEachIndexed { i, l ->
                val left = right - widths[i]
                if (fill != null) canvas.drawRect(left, y, right, y + h, fill)
                canvas.drawRect(left, y, right, y + h, line)
                canvas.save()
                canvas.translate(left + PAD, y + PAD)
                l.draw(canvas)
                canvas.restore()
                right = left
            }
            y += h
        }

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            y = MARGIN
            if (pageNo == 1) {
                val t = layout(title, titlePaint, PAGE_W.toFloat())
                page!!.canvas.save(); page!!.canvas.translate(0f, y); t.draw(page!!.canvas); page!!.canvas.restore()
                y += t.height + 2
                val s = layout(subtitle, subPaint, PAGE_W.toFloat())
                page!!.canvas.save(); page!!.canvas.translate(0f, y); s.draw(page!!.canvas); page!!.canvas.restore()
                y += s.height + 8
            }
            drawRow(headLayouts, headH, headFill)
        }

        newPage()
        bodyLayouts.forEach { ls ->
            val h = heightOf(ls)
            if (y + h > PAGE_H - MARGIN) newPage()
            drawRow(ls, h, null)
        }
        doc.finishPage(page!!)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
