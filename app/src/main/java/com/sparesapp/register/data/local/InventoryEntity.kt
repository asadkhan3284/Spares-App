package com.sparesapp.register.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.data.model.StockStatus

/** Offline cache of the last successfully loaded spreadsheet. Photos are never cached here — fetched live. */
@Entity(tableName = "inventory_rows")
data class InventoryEntity(
    @PrimaryKey val mat: String,
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
    val statusKey: String,
)

fun InventoryRow.toEntity(): InventoryEntity = InventoryEntity(
    mat = mat, old = old, sd = sd, ld = ld, qty = qty, proj = proj, res = res, ss = ss, rop = rop,
    unit = unit, bom = bom, mrp = mrp, price = price, lastIss = lastIss, lastRec = lastRec,
    yearly = yearly, whereUsed = whereUsed, area = area, bomAval = bomAval, ropStatus = ropStatus,
    ropStatusProj = ropStatusProj, ordering = ordering, prStatus = prStatus, poStatus = poStatus,
    underInsp = underInsp, over15 = over15, invValue = invValue, statusKey = status.key,
)

fun InventoryEntity.toRow(): InventoryRow = InventoryRow(
    mat = mat, old = old, sd = sd, ld = ld, qty = qty, proj = proj, res = res, ss = ss, rop = rop,
    unit = unit, bom = bom, mrp = mrp, price = price, lastIss = lastIss, lastRec = lastRec,
    yearly = yearly, whereUsed = whereUsed, area = area, bomAval = bomAval, ropStatus = ropStatus,
    ropStatusProj = ropStatusProj, ordering = ordering, prStatus = prStatus, poStatus = poStatus,
    underInsp = underInsp, over15 = over15, invValue = invValue,
    status = StockStatus.entries.firstOrNull { it.key == statusKey } ?: StockStatus.OK,
)
