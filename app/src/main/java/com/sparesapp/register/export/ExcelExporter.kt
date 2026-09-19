package com.sparesapp.register.export

import android.content.Context
import android.net.Uri
import com.sparesapp.register.data.model.ExportColumn
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.data.model.exportValue
import org.dhatim.fastexcel.Workbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes filtered rows to a brand-new local .xlsx (picked by the user via
 * Storage Access Framework). This never touches the original OneDrive/
 * SharePoint source file — the app is a read-only viewer of that source.
 */
object ExcelExporter {

    fun suggestedFileName(prefix: String = "spares_extract"): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "${prefix}_$stamp.xlsx"
    }

    fun export(context: Context, uri: Uri, rows: List<InventoryRow>): Result<Int> = runCatching {
        val resolver = context.contentResolver
        resolver.openOutputStream(uri)?.use { os ->
            Workbook(os, "Spares Register", "1.0").use { wb ->
                val ws = wb.newWorksheet("Extract")
                val columns = ExportColumn.entries
                columns.forEachIndexed { c, col -> ws.value(0, c, col.label) }
                rows.forEachIndexed { r, row ->
                    columns.forEachIndexed { c, col ->
                        ws.value(r + 1, c, row.exportValue(col))
                    }
                }
            }
        } ?: throw IllegalStateException("Could not open the chosen file for writing")
        rows.size
    }
}
