package com.omniweb.app.util.adblock
import com.omniweb.app.util.adblock.hash.HashingAlgorithm
import com.omniweb.app.util.adblock.integer.lowerHalf
import com.omniweb.app.util.adblock.integer.upperHalf
import java.io.Serializable
import java.util.*
import kotlin.math.ln
import kotlin.math.roundToInt
interface BloomFilter<T> {
    fun put(item: T)
    fun putAll(collection: Collection<T>)
    fun mightContain(item: T): Boolean
}
class DefaultBloomFilter<T>(
    numberOfElements: Int,
    falsePositiveRate: Double,
    private val hashingAlgorithm: HashingAlgorithm<T>
) : BloomFilter<T>, Serializable {
    private val numberOfBits: Int = (-numberOfElements * ln(falsePositiveRate) / (ln(2.0) * ln(2.0))).roundToInt().coerceAtLeast(1)
    private val numberOfHashes: Int = (numberOfBits * ln(2.0) / numberOfElements).roundToInt().coerceAtLeast(1)
    private val bitSet: BitSet = BitSet(numberOfBits)
    override fun put(item: T) {
        val hash = hashingAlgorithm.hash(item)
        var combinedHash = hash.lowerHalf()
        val upperHalf = hash.upperHalf()
        for (i in 0 until numberOfHashes) {
            bitSet.set((combinedHash and 0x7FFFFFFF) % numberOfBits)
            combinedHash += upperHalf
        }
    }
    override fun putAll(collection: Collection<T>) { collection.forEach(::put) }
    override fun mightContain(item: T): Boolean {
        val hash = hashingAlgorithm.hash(item)
        var combinedHash = hash.lowerHalf()
        val upperHalf = hash.upperHalf()
        for (i in 0 until numberOfHashes) {
            if (!bitSet.get((combinedHash and 0x7FFFFFFF) % numberOfBits)) return false
            combinedHash += upperHalf
        }
        return true
    }
}
