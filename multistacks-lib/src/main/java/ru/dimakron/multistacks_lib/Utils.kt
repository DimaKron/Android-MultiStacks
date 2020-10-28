package ru.dimakron.multistacks_lib

internal fun <T> MutableList<T>.replaceWith(items: List<T>){
    clear()
    addAll(items)
}