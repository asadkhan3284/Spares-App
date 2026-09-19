package com.sparesapp.register.data

import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.data.model.StockStatus
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.ByteArrayInputStream

class InventoryParseException(message: String) : Exception(message)

/**
 * Direct Kotlin port of the original HTML tool's parsing pipeline:
 * matchHeader() + parseWorkbookRows() + deriveStatus(), same column
 * aliases, same "first matching header wins" behavior, same status rules.
 */
object InventoryParser {

    // Mirrors HEADER_ALIASES exactly (lowercase, trailing "." stripped, as matchHeader() does to the file's headers).
    private val HEADER_ALIASES: Map<String, List<String>> = mapOf(
        "mat" to listOf("material", "material #", "material#", "material no", "material number", "material no."),
        "old" to listOf("old material number", "old material #", "old material no", "old material no.", "old #", "old number"),
        "sd" to listOf("short description", "short desc", "short desc."),
        "ld" to listOf("long description", "long desc", "long desc."),
        "qty" to listOf("qty unrestricted", "qty on hand", "on hand qty", "unrestricted qty", "qty available", "on hand"),
        "proj" to listOf("qty project"),
        "res" to listOf("qty reserved"),
        "ss" to listOf("safety stock"),
        "rop" to listOf("rop", "reorder point"),
        "unit" to listOf("base unit", "unit", "uom"),
        "bom" to listOf("bom"),
        "mrp" to listOf("mrp controller desc.", "mrp controller desc", "mrp controller"),
        "price" to listOf("moving avg price", "moving average price", "price"),
        "lastiss" to listOf("last issuance date"),
        "lastrec" to listOf("last receiving date"),
        "yearly" to listOf("yearly usage"),
        "whereused" to listOf("whereused", "where used"),
        "area" to listOf("qm auth. group desc.", "qm auth group desc", "qm auth. group desc", "responsible area", "area"),
        "bomAval" to listOf("bom aval.", "bom aval", "bom availability"),
        "ropStatus" to listOf("rop status"),
        "ropStatusProj" to listOf("rop status (un+prj)", "rop status un+prj", "rop status (incl project)", "rop status incl project"),
        "ordering" to listOf("pr/po status", "ordering status"),
        "prStatus" to listOf("pr status"),
        "poStatus" to listOf("po status"),
        "underInsp" to listOf("under inspection"),
        "over15" to listOf("over 15 days", "under insp (>15 days)", "under inspection (>15 days)"),
        "invValue" to listOf("inventory value"),
    )

    fun cleanStr(v: String?): String = v?.replace(Regex("\\s{2,}"), " ")?.trim().orEmpty()

    fun toFloat(v: String?): Double? {
        if (v.isNullOrBlank()) return null
        val s = v.replace(",", "").trim()
        if (s.isEmpty() || s.equals("na", ignoreCase = true)) return null
        return s.toDoubleOrNull()
    }

    private fun matchHeader(headerRow: List<String>): Map<String, Int> {
        val normHeaders = headerRow.map { cleanStr(it).lowercase().let { s -> if (s.endsWith(".")) s.dropLast(1) else s } }
        val colIndex = mutableMapOf<String, Int>()
        HEADER_ALIASES.forEach { (key, aliases) ->
            for (i in normHeaders.indices) {
                if (normHeaders[i] in aliases) { colIndex[key] = i; break }
            }
        }
        return colIndex
    }

    /** rows: raw string grid, rows[0] is the header row (matches SheetJS's {header:1, raw:true, defval:''}). */
    fun parseRows(rows: List<List<String>>): List<InventoryRow> {
        if (rows.isEmpty()) return emptyList()
        val colIndex = matchHeader(rows[0])
        if ("mat" !in colIndex) {
            throw InventoryParseException("Could not find a \"Material #\" column in this file — check it has the expected headers.")
        }
        fun get(row: List<String>, key: String): String? = colIndex[key]?.let { row.getOrNull(it) }

        val out = mutableListOf<InventoryRow>()
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue
            val mat = cleanStr(get(row, "mat"))
            if (mat.isBlank()) continue
            val qty = toFloat(get(row, "qty"))
            val ss = toFloat(get(row, "ss"))
            val rop = toFloat(get(row, "rop"))
            out += InventoryRow(
                mat = mat,
                old = cleanStr(get(row, "old")),
                sd = cleanStr(get(row, "sd")),
                ld = cleanStr(get(row, "ld")),
                qty = qty,
                proj = toFloat(get(row, "proj")),
                res = toFloat(get(row, "res")),
                ss = ss,
                rop = rop,
                unit = cleanStr(get(row, "unit")),
                bom = cleanStr(get(row, "bom")),
                mrp = cleanStr(get(row, "mrp")),
                price = toFloat(get(row, "price")),
                lastIss = cleanStr(get(row, "lastiss")),
                lastRec = cleanStr(get(row, "lastrec")),
                yearly = toFloat(get(row, "yearly")),
                whereUsed = cleanStr(get(row, "whereused")),
                area = cleanStr(get(row, "area")),
                bomAval = cleanStr(get(row, "bomAval")),
                ropStatus = cleanStr(get(row, "ropStatus")),
                ropStatusProj = cleanStr(get(row, "ropStatusProj")),
                ordering = cleanStr(get(row, "ordering")),
                prStatus = cleanStr(get(row, "prStatus")),
                poStatus = cleanStr(get(row, "poStatus")),
                underInsp = cleanStr(get(row, "underInsp")),
                over15 = cleanStr(get(row, "over15")),
                invValue = toFloat(get(row, "invValue")),
                status = StockStatus.derive(qty, ss, rop),
            )
        }
        if (out.isEmpty()) throw InventoryParseException("No usable rows found in this file.")
        return out
    }

    fun parseXlsx(bytes: ByteArray): List<InventoryRow> {
        val grid = mutableListOf<List<String>>()
        ByteArrayInputStream(bytes).use { input ->
            ReadableWorkbook(input).use { wb ->
                val sheet = wb.firstSheet
                val rows = sheet.read()
                for (row in rows) {
                    val width = row.cellCount
                    grid += (0 until width).map { idx -> row.getCellText(idx) ?: "" }
                }
            }
        }
        return parseRows(grid)
    }

    fun parseCsv(bytes: ByteArray): List<InventoryRow> {
        val text = bytes.toString(Charsets.UTF_8)
        return parseRows(CsvParser.parse(text))
    }
}

/** Minimal RFC4180-style CSV parser: handles quoted fields, embedded commas/newlines, and "" escapes. */
object CsvParser {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        val n = text.length

        fun endField() { row.add(field.toString()); field = StringBuilder() }
        fun endRow() { endField(); rows.add(row); row = mutableListOf() }

        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> endField()
                    '\r' -> {}
                    '\n' -> endRow()
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}
