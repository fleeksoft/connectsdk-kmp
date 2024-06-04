package com.fleeksoft.connectsdk.core

/** Normalized reference object for information about a TVs program.  */
data class ProgramInfo(
    /** Gets the ID of the program on the first screen device. Format is different depending on the platform.  */
    /** Sets the ID of the program on the first screen device. Format is different depending on the platform.  */
    // @cond INTERNAL
    var id: String? = null,
    /** Gets the user-friendly name of the program (ex. Sesame Street, Cosmos, Game of Thrones, etc).  */
    /** Sets the user-friendly name of the program (ex. Sesame Street, Cosmos, Game of Thrones, etc).  */
    var name: String? = null,

    /** Gets the reference to the ChannelInfo object that this program is associated with  */
    /** Sets the reference to the ChannelInfo object that this program is associated with  */
    var channelInfo: ChannelInfo? = null,
) {
    /**
     * Compares two ProgramInfo objects.
     *
     * @param programInfo ProgramInfo object to compare.
     *
     * @return true if both ProgramInfo id & name values are equal
     */
    override fun equals(other: Any?): Boolean {
        if (other is ProgramInfo) {
            return this.id == other.id && this.name == other.name
        }
        return super.equals(other)
    }
}
