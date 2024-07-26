import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleeksoft.connectsdk.core.MediaInfo
import com.fleeksoft.connectsdk.device.ConnectableDevice
import com.fleeksoft.connectsdk.device.ConnectableDeviceListener
import com.fleeksoft.connectsdk.discovery.DiscoveryManager
import com.fleeksoft.connectsdk.discovery.DiscoveryManagerListener
import com.fleeksoft.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.fleeksoft.connectsdk.service.DLNAService
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.capability.MediaControl
import com.fleeksoft.connectsdk.service.capability.MediaPlayer
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

var mediaPlaying: Boolean = false
var mediaControl: MediaControl? = null

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var serviceStarted by remember { mutableStateOf(false) }
    var videoPlaying by remember { mutableStateOf(false) }
    DiscoveryManager.init()
    val discoveryManager = DiscoveryManager.getInstance()
    scope.launch(Dispatchers.IO) {
        discoveryManager.registerDeviceService(DLNAService.getServiceProvider(), lazy { SSDPDiscoveryProvider.instance })
        discoveryManager.addListener(object : DiscoveryManagerListener {
            override fun onDeviceAdded(manager: DiscoveryManager, device: ConnectableDevice) {
                if (mediaPlaying) return
                mediaPlaying = true
                println("device added: $device")
                device.addListener(object : ConnectableDeviceListener {
                    override fun onDeviceReady(device: ConnectableDevice) {
                        println("device ready: $device")
                        val player = device.getCapability(MediaPlayer::class)
                        val mediaInfo =
                            MediaInfo.Builder("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "video/mp4")
                                .build()
                        scope.launch(Dispatchers.IO) {
                            println("Play media: ${mediaInfo.url}")
                            player?.playMedia(mediaInfo, false, object : MediaPlayer.LaunchListener {
                                override suspend fun onSuccess(response: MediaPlayer.MediaLaunchObject?) {
                                    mediaControl = response?.mediaControl
                                    videoPlaying = true
                                    println("playMedia#onSuccess: $response")
                                }

                                override suspend fun onError(error: ServiceCommandError) {
                                    println("playMedia#onError: $error")
                                }

                            })
                        }
                    }

                    override fun onDeviceDisconnected(device: ConnectableDevice) {
                        println("onDeviceDisconnected: $device")
                    }

                    override fun onPairingRequired(
                        device: ConnectableDevice,
                        service: DeviceService,
                        pairingType: DeviceService.PairingType
                    ) {
                        println("onPairingRequired: $device")
                    }

                    override suspend fun onCapabilityUpdated(
                        device: ConnectableDevice,
                        added: List<String>,
                        removed: List<String>
                    ) {
                        println("onCapabilityUpdated: $device")
                    }

                    override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
                        println("onConnectionFailed: $device")
                    }

                })

                scope.launch(Dispatchers.IO) { device.connect() }
            }

            override fun onDeviceUpdated(manager: DiscoveryManager, device: ConnectableDevice) {
                println("device updated: $device")
            }

            override fun onDeviceRemoved(manager: DiscoveryManager, device: ConnectableDevice) {
                println("device removed: $device")
            }

            override fun onDiscoveryFailed(manager: DiscoveryManager, error: ServiceCommandError) {
                println("onDiscoveryFailed: $error")
            }

        })
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp)) {
            BasicText("Service Started: ")
            Switch(serviceStarted,
                onCheckedChange = {
                    serviceStarted = it
                    if (serviceStarted) {
                        scope.launch(Dispatchers.IO) { discoveryManager.start() }
                    } else {
                        scope.launch(Dispatchers.IO) { discoveryManager.stop() }
                    }
                })
        }

        Row {
            IconButton({
                scope.launch {
                    if (videoPlaying) {
                        mediaControl?.pause(null)
                    } else {
                        mediaControl?.play(null)
                    }

                    videoPlaying = !videoPlaying
                }
            }) {
                Icon(if (videoPlaying) Icons.Default.Close else Icons.Default.PlayArrow, "Play")
            }
        }
    }
}