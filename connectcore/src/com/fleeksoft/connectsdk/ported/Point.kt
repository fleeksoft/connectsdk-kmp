package com.fleeksoft.connectsdk.ported

class Point {
    var x: Int = 0
    var y: Int = 0

    constructor()

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    constructor(src: Point) {
        set(src)
    }

    /**
     * Set the point's x and y coordinates
     */
    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    /**
     * Sets the point's from `src`'s coordinates
     * @hide
     */
    fun set(src: Point) {
        this.x = src.x
        this.y = src.y
    }

    /**
     * Negate the point's coordinates
     */
    fun negate() {
        x = -x
        y = -y
    }

    /**
     * Offset the point's coordinates by dx, dy
     */
    fun offset(dx: Int, dy: Int) {
        x += dx
        y += dy
    }

    /**
     * Returns true if the point's coordinates equal (x,y)
     */
    fun equals(x: Int, y: Int): Boolean {
        return this.x == x && this.y == y
    }

    override fun toString(): String {
        return "Point($x, $y)"
    }

    /**
     * @return Returns a [String] that represents this point which can be parsed with
     * [.unflattenFromString].
     * @hide
     */
    fun flattenToString(): String {
        return x.toString() + "x" + y
    }

    /**
     * Parcelable interface methods
     */
    fun describeContents(): Int {
        return 0
    }


    companion object {
        /**
         * @return Returns a [Point] from a short string created from [.flattenToString].
         * @hide
         */
        @Throws(NumberFormatException::class)
        fun unflattenFromString(s: String): Point {
            val sep_ix = s.indexOf("x")
            return Point(
                s.substring(0, sep_ix).toInt(),
                s.substring(sep_ix + 1).toInt()
            )
        }
    }
}