package io.cyborgsquirrel.util

fun <T> List<T>.shift(amount: Int): List<T> {
    val newList = mutableListOf<T>()
    for (i in this.indices) {
        newList.add(this[(i + amount) % this.size])
    }

    return newList
}