package com.fleeksoft.connectsdk.service.capability

import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceSubscription

interface MediaControl : CapabilityMethods {
    /**
     * Enumerates possible playback status
     */
    enum class PlayStateStatus {
        /**
         * Unknown state
         */
        Unknown,

        /**
         * Media source is not set.
         */
        Idle,

        /**
         * Media is playing.
         */
        Playing,

        /**
         * Media is paused.
         */
        Paused,

        /**
         * Media is buffering on the first screen device (e.g. on the TV)
         */
        Buffering,

        /**
         * Playback is finished.
         */
        Finished;

        companion object {
            /**
             * Converts int value into PlayStateStatus
             * @param playerState int value
             * @return PlayStateStatus
             */
            fun convertPlayerStateToPlayStateStatus(playerState: Int): PlayStateStatus {
                var status: PlayStateStatus? = Unknown

                status = when (playerState) {
                    PLAYER_STATE_BUFFERING -> Buffering
                    PLAYER_STATE_IDLE -> Finished
                    PLAYER_STATE_PAUSED -> Paused
                    PLAYER_STATE_PLAYING -> Playing
                    PLAYER_STATE_UNKNOWN -> Unknown
                    else -> Unknown
                }
                return status
            }

            /**
             * Converts String value into PlayStateStatus
             * @param transportState String value
             * @return PlayStateStatus
             */
            fun convertTransportStateToPlayStateStatus(transportState: String): PlayStateStatus {
                var status: PlayStateStatus = Unknown

                if ((transportState == "STOPPED")) {
                    status = Finished
                } else if ((transportState == "PLAYING")) {
                    status = Playing
                } else if ((transportState == "TRANSITIONING")) {
                    status = Buffering
                } else if ((transportState == "PAUSED_PLAYBACK")) {
                    status = Paused
                } else if ((transportState == "PAUSED_RECORDING")) {
                } else if ((transportState == "RECORDING")) {
                } else if ((transportState == "NO_MEDIA_PRESENT")) {
                }
                return status
            }
        }
    }

    /**
     * Get MediaControl implementation
     * @return MediaControl
     */
    fun getMediaControl(): MediaControl?

    /**
     * Get a capability priority for current implementation
     * @return CapabilityPriorityLevel
     */
    fun getMediaControlCapabilityLevel(): CapabilityPriorityLevel?

    suspend fun play(listener: ResponseListener<Any?>)

    suspend fun pause(listener: ResponseListener<Any?>)

    suspend fun stop(listener: ResponseListener<Any?>)

    suspend fun rewind(listener: ResponseListener<Any?>)

    suspend fun fastForward(listener: ResponseListener<Any?>)

    /**
     * @param position The new position, in milliseconds from the beginning of the stream
     * @param listener (optional) ResponseListener< Object > with methods to be called on success
     * or failure
     */
    suspend fun seek(position: Long, listener: ResponseListener<Any?>)

    /**
     * Get the current media duration in milliseconds
     */
    suspend fun getDuration(listener: DurationListener)

    /**
     * Get the current playback position in milliseconds
     */
    suspend fun getPosition(listener: PositionListener)

    /**
     * Get the current state of playback
     */
    suspend fun getPlayState(listener: PlayStateListener)

    /**
     * Subscribe for playback state changes
     * @param listener receives play state notifications
     * @return ServiceSubscription<PlayStateListener>
    </PlayStateListener> */
    suspend fun subscribePlayState(listener: PlayStateListener): ServiceSubscription<PlayStateListener>?

    /**
     * Success block that is called upon any change in a media file's play state.
     *
     * Passes a PlayStateStatus enum of the current media file
     */
    interface PlayStateListener : ResponseListener<PlayStateStatus?>

    /**
     * Success block that is called upon successfully getting the media file's current playhead position.
     *
     * Passes the position of the current playhead position of the current media file, in seconds
     */
    interface PositionListener : ResponseListener<Long?>

    /**
     * Success block that is called upon successfully getting the media file's duration.
     *
     * Passes the duration of the current media file, in seconds
     */
    interface DurationListener : ResponseListener<Long?>
    companion object {
        val Any: String = "MediaControl.Any"

        val Play: String = "MediaControl.Play"
        val Pause: String = "MediaControl.Pause"
        val Stop: String = "MediaControl.Stop"
        val Rewind: String = "MediaControl.Rewind"
        val FastForward: String = "MediaControl.FastForward"
        val Seek: String = "MediaControl.Seek"
        val Duration: String = "MediaControl.Duration"
        val PlayState: String = "MediaControl.PlayState"
        val PlayState_Subscribe: String = "MediaControl.PlayState.Subscribe"
        val Position: String = "MediaControl.Position"


        val PLAYER_STATE_UNKNOWN: Int = 0
        val PLAYER_STATE_IDLE: Int = 1
        val PLAYER_STATE_PLAYING: Int = 2
        val PLAYER_STATE_PAUSED: Int = 3
        val PLAYER_STATE_BUFFERING: Int = 4


        val Capabilities: Array<String> = arrayOf(
            Play,
            Pause,
            Stop,
            Rewind,
            FastForward,
            Seek,
            PlaylistControl.Previous,
            PlaylistControl.Next,
            Duration,
            PlayState,
            PlayState_Subscribe,
            Position,
        )
    }
}
