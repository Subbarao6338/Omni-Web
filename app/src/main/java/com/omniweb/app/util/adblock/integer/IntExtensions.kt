package com.omniweb.app.util.adblock.integer
fun Int.lowerHalf(): Int {
    val half = (Int.SIZE_BITS / 2)
    return (this shl half) ushr half
}
fun Int.upperHalf(): Int {
    val half = (Int.SIZE_BITS / 2)
    return (this ushr half) shl half
}
