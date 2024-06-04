package com.fleeksoft.connectsdk.service.capability

import com.fleeksoft.connectsdk.core.ExternalInputInfo
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.Launcher.AppLaunchListener
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.sessions.LaunchSession

interface ExternalInputControl : CapabilityMethods {
    fun getExternalInput(): ExternalInputControl
    fun getExternalInputControlPriorityLevel(): CapabilityPriorityLevel?

    fun launchInputPicker(listener: AppLaunchListener)
    fun closeInputPicker(launchSessionm: LaunchSession?, listener: ResponseListener<Any?>)

    fun getExternalInputList(listener: ExternalInputListListener?)
    fun setExternalInput(input: ExternalInputInfo?, listener: ResponseListener<Any?>?)

    /**
     * Success block that is called upon successfully getting the external input list.
     *
     * Passes a list containing an ExternalInputInfo object for each available external input on the device
     */
    interface ExternalInputListListener : ResponseListener<List<ExternalInputInfo?>?>
    companion object {
        val Any: String = "ExternalInputControl.Any"

        val Picker_Launch: String = "ExternalInputControl.Picker.Launch"
        val Picker_Close: String = "ExternalInputControl.Picker.Close"
        val List: String = "ExternalInputControl.List"
        val Set: String = "ExternalInputControl.Set"

        val Capabilities: Array<String> = arrayOf(
            Picker_Launch,
            Picker_Close,
            List,
            Set
        )
    }
}
