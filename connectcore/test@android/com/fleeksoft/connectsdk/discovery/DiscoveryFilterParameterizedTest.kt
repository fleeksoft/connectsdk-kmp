package com.fleeksoft.connectsdk.discovery

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Created by oleksii.frolov on 2/12/2015.
 */
@RunWith(Parameterized::class)
class DiscoveryFilterParameterizedTest(
    private val mIdA: String,
    private val mFilterA: String,
    private val mIdB: String,
    private val mFilterB: String,
    private val mResult: Boolean
) {
    @Test
    fun testEquals() {
        val filterA = DiscoveryFilter(mIdA, mFilterA)
        val filterB = DiscoveryFilter(mIdB, mFilterB)
        Assert.assertEquals(mResult, filterA == filterB)
        Assert.assertEquals(mResult, filterB == filterA)
    }

    @Test
    fun testHashCode() {
        val filterA = DiscoveryFilter(mIdA, mFilterA)
        val filterB = DiscoveryFilter(mIdB, mFilterB)
        Assert.assertEquals(mResult, filterA.hashCode() == filterB.hashCode())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun configs(): List<Array<Any?>> {
            return listOf(
                arrayOf("id", "filter", "id", "filter", true),
                arrayOf("id", "filter", "id", "another", false),
                arrayOf("id", "filter", "another", "filter", false),
                arrayOf("id", "filter", "null", "filter", false),
            )
        }
    }
}
