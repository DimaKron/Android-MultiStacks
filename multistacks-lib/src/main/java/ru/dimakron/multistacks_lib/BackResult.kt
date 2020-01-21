package ru.dimakron.multistacks_lib

data class BackResult(val type: BackResultType,
                      val newIndex: Int? = null)

enum class BackResultType{
    OK, CANCELLED
}