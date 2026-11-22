package ru.nsu.habitgrid.ui.components

fun isRussianGenitiveSingular(count: Int): Boolean = count % 10 == 1 && count % 100 != 11
