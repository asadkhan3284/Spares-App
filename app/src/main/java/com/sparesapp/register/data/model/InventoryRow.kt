package com.sparesapp.register.data.model

/**
 * Mirrors the row shape produced by parseWorkbookRows() in the original
 * Spares Register HTML tool — same field names, same source columns.
 */
data class InventoryRow(
    val mat: String,
    val old: String,
    val sd: String,
    val ld: String,
    val qty: Double?,
    val proj: Double?,
    val res: Double?,
    val ss: Double?,
    val rop: Double?,
    val unit: String,
    val bom: String,
    val mrp: String,
    val price: Double?,
    val lastIss: String,
    val lastRec: String,
    val yearly: Double?,
    val whereUsed: String,
    val area: String,
    val bomAval: String,
    val ropStatus: String,
    val ropStatusProj: String,
    val ordering: String,
    val prStatus: String,
    val poStatus: String,
    val underInsp: String,
    val over15: String,
    val invValue: Double?,
    // Derived
    val status: StockStatus = StockStatus.OK,
    val images: List<PhotoRef> = emptyList(),
) {
    val hasImages: Boolean get() = images.isNotEmpty()

    val searchBlob: String by lazy {
        listOf(mat, old, sd, ld, bom, mrp).filter { it.isNotBlank() }.joinToString(" ").lowercase()
    }
}

/** A photo matched to a row, always addressed as a Graph (driveId, itemId) pair — never persisted, fetched fresh. */
data class PhotoRef(val driveId: String, val itemId: String, val name: String)

enum class StockStatus(val key: String, val label: String) {
    ZERO("zero", "Zero stock"),
    BELOW_SAFETY_STOCK("below_ss", "Below safety stock"),
    BELOW_ROP("below_rop", "Below ROP"),
    OK("ok", "Healthy stock");

    companion object {
        /** Ports deriveStatus() exactly: qty<=0 -> zero; qty<ss -> below_ss; qty<rop -> below_rop; else ok. */
        fun derive(qty: Double?, ss: Double?, rop: Double?): StockStatus {
            val q = qty ?: 0.0
            if (q <= 0.0) return ZERO
            if (ss != null && q < ss) return BELOW_SAFETY_STOCK
            if (rop != null && q < rop) return BELOW_ROP
            return OK
        }
    }
}

/** Column keys in the same order/labels as EXPORT_COLUMNS in the original tool. */
enum class ExportColumn(val label: String) {
    MAT("Material #"), OLD("Old Material #"), SD("Short Description"), LD("Long Description"),
    QTY("Qty On Hand"), UNIT("Unit"), SS("Safety Stock"), ROP("ROP"),
    PROJ("Qty Project"), RES("Qty Reserved"), BOM("BOM"), MRP("Spares Category"),
    AREA("Responsible Area"), BOM_AVAL("BOM Available"), ROP_STATUS("ROP Status (Excl. Project)"),
    ROP_STATUS_PROJ("ROP Status (Incl. Project)"), ORDERING("Ordering Status"), PR_STATUS("PR Status"),
    PO_STATUS("PO Status"), UNDER_INSP("Under Inspection"), OVER_15("Under Inspection >15 Days"),
    INV_VALUE("Inventory Value"), PRICE("Moving Avg Price"), YEARLY("Yearly Usage"),
    LAST_ISS("Last Issuance Date"), LAST_REC("Last Receiving Date"), WHERE_USED("Where Used");
}

fun InventoryRow.exportValue(column: ExportColumn): String = when (column) {
    ExportColumn.MAT -> mat
    ExportColumn.OLD -> old
    ExportColumn.SD -> sd
    ExportColumn.LD -> ld
    ExportColumn.QTY -> qty?.let { fmtNum(it) } ?: ""
    ExportColumn.UNIT -> unit
    ExportColumn.SS -> ss?.let { fmtNum(it) } ?: ""
    ExportColumn.ROP -> rop?.let { fmtNum(it) } ?: ""
    ExportColumn.PROJ -> proj?.let { fmtNum(it) } ?: ""
    ExportColumn.RES -> res?.let { fmtNum(it) } ?: ""
    ExportColumn.BOM -> bom
    ExportColumn.MRP -> mrp
    ExportColumn.AREA -> area
    ExportColumn.BOM_AVAL -> bomAval
    ExportColumn.ROP_STATUS -> ropStatus
    ExportColumn.ROP_STATUS_PROJ -> ropStatusProj
    ExportColumn.ORDERING -> ordering
    ExportColumn.PR_STATUS -> prStatus
    ExportColumn.PO_STATUS -> poStatus
    ExportColumn.UNDER_INSP -> underInsp
    ExportColumn.OVER_15 -> over15
    ExportColumn.INV_VALUE -> invValue?.let { fmtNum(it) } ?: ""
    ExportColumn.PRICE -> price?.let { fmtNum(it) } ?: ""
    ExportColumn.YEARLY -> yearly?.let { fmtNum(it) } ?: ""
    ExportColumn.LAST_ISS -> lastIss
    ExportColumn.LAST_REC -> lastRec
    ExportColumn.WHERE_USED -> whereUsed
}

private fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
