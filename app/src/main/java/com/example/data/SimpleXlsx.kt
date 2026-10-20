package com.example.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Just enough of the .xlsx format to read the first sheet of a workbook as rows of text and to
 * write a single right-to-left sheet — the same files the web app imports and exports, without a
 * heavy spreadsheet library in the app.
 */
object SimpleXlsx {
    /** UTF-8 byte-order mark, so Excel reads Arabic CSV correctly. */
    private val BOM = Char(0xFEFF).toString()


    /** Rows of the first sheet, each a list of cell texts (missing cells are ""). */
    fun read(bytes: ByteArray): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (!e.isDirectory && (e.name == "xl/sharedStrings.xml" || e.name.startsWith("xl/worksheets/sheet") || e.name == "xl/workbook.xml")) {
                    entries[e.name] = zip.readBytes()
                }
                e = zip.nextEntry
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetName = entries.keys.filter { it.startsWith("xl/worksheets/sheet") }.minByOrNull { name ->
            name.removePrefix("xl/worksheets/sheet").removeSuffix(".xml").toIntOrNull() ?: Int.MAX_VALUE
        } ?: return emptyList()
        return parseSheet(entries.getValue(sheetName), shared)
    }

    private fun parser(bytes: ByteArray): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(ByteArrayInputStream(bytes), "UTF-8")
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val out = mutableListOf<String>()
        val p = parser(bytes)
        var current: StringBuilder? = null
        var inText = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> current = StringBuilder()
                    "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText) current?.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inText = false
                    "si" -> { out.add(current?.toString() ?: ""); current = null }
                }
            }
        }
        return out
    }

    /** "C12" -> 2 (zero-based column). */
    private fun columnIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (!ch.isLetter()) break
            n = n * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return n - 1
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val p = parser(bytes)
        var row: MutableMap<Int, String>? = null
        var cellCol = 0
        var cellType: String? = null
        var value = StringBuilder()
        var inValue = false
        var nextCol = 0
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> { row = mutableMapOf(); nextCol = 0 }
                    "c" -> {
                        val ref = p.getAttributeValue(null, "r")
                        cellCol = if (ref != null) columnIndex(ref) else nextCol
                        cellType = p.getAttributeValue(null, "t")
                        value = StringBuilder()
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val raw = value.toString()
                        val text = when (cellType) {
                            "s" -> raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                            "b" -> if (raw == "1") "TRUE" else "FALSE"
                            else -> raw.removeSuffix(".0")
                        }
                        row?.put(cellCol, text)
                        nextCol = cellCol + 1
                    }
                    "row" -> {
                        val r = row ?: continue
                        val width = (r.keys.maxOrNull() ?: -1) + 1
                        rows.add((0 until width).map { r[it] ?: "" })
                        row = null
                    }
                }
            }
        }
        return rows
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun colName(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.insert(0, ('A' + r))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    /** A workbook with one right-to-left sheet; numbers stay numbers, everything else is text. */
    fun write(sheetName: String, header: List<String>, rows: List<List<Any?>>, columnWidths: List<Int> = emptyList()): ByteArray {
        val sheet = StringBuilder()
        sheet.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sheet.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView rightToLeft="1" workbookViewId="0"/></sheetViews>""")
        if (columnWidths.isNotEmpty()) {
            sheet.append("<cols>")
            columnWidths.forEachIndexed { i, w -> sheet.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            sheet.append("</cols>")
        }
        sheet.append("<sheetData>")
        (listOf(header) + rows).forEachIndexed { r, cells ->
            sheet.append("""<row r="${r + 1}">""")
            cells.forEachIndexed { c, v ->
                val ref = "${colName(c)}${r + 1}"
                when (v) {
                    is Number -> sheet.append("""<c r="$ref"><v>$v</v></c>""")
                    null -> {}
                    else -> sheet.append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${esc(v.toString())}</t></is></c>""")
                }
            }
            sheet.append("</row>")
        }
        sheet.append("</sheetData></worksheet>")

        val files = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${esc(sheetName.take(31))}" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/worksheets/sheet1.xml" to sheet.toString()
        )
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** CSV with a UTF-8 BOM so Excel shows Arabic correctly (web: exportCourseRowsToCSV). */
    fun csv(header: List<String>, rows: List<List<Any?>>): ByteArray {
        fun cell(v: Any?): String {
            val s = v?.toString() ?: ""
            // Formula-looking cells are neutralised, as on the web (csvCell).
            val safe = if (s.isNotEmpty() && s[0] in "=+-@\t\r") "'$s" else s
            return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
        }
        val text = (listOf(header) + rows).joinToString("\r\n") { r -> r.joinToString(",") { cell(it) } }
        return (BOM + text).toByteArray(Charsets.UTF_8)
    }

    /** Parses CSV text (quotes, commas and new lines inside quotes). */
    fun parseCsv(bytes: ByteArray): List<List<String>> {
        val text = String(bytes, Charsets.UTF_8).removePrefix(BOM)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (quoted) {
                if (ch == '"' && i + 1 < text.length && text[i + 1] == '"') { cell.append('"'); i++ }
                else if (ch == '"') quoted = false
                else cell.append(ch)
            } else when (ch) {
                '"' -> quoted = true
                ',' -> { row.add(cell.toString()); cell.clear() }
                '\r' -> {}
                '\n' -> { row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf() }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}
