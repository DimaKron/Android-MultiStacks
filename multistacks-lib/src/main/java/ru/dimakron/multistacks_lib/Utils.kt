package ru.dimakron.multistacks_lib

fun <T> MutableList<T>.replaceWith(items: List<T>){
    clear()
    addAll(items)
}