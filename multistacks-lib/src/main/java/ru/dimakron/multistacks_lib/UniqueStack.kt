package ru.dimakron.multistacks_lib

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class UniqueStack(private val elements: MutableList<Int> = mutableListOf()): Parcelable{

    fun push(entry: Int) {
        elements.remove(entry)
        elements.add(entry)
    }

    fun pop() = if(elements.isNotEmpty()) elements.removeAt(elements.size - 1) else null

    fun peek() = if(elements.isNotEmpty()) elements[elements.size - 1] else null

    fun isEmpty() = elements.isEmpty()

    fun getSize() = elements.size

    fun clear() {
        elements.clear()
    }
}