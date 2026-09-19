package com.sparesapp.register.ui.common

import java.text.NumberFormat
import java.util.Locale

private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 2
}

fun fmtQty(v: Double?): String = if (v == null) "—" else numberFormat.format(v)
fun fmtMoney(v: Double?): String = if (v == null) "—" else numberFormat.format(v)
