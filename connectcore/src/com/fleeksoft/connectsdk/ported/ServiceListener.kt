package com.fleeksoft.connectsdk.ported

import com.fleeksoft.connectsdk.ported.mdns.ServiceEvent

interface ServiceListener {
    /**
     * A service has been added.<br></br>
     * **Note:**This event is only the service added event. The service info associated with this event does not include resolution information.<br></br>
     * To get the full resolved information you need to listen to [.serviceResolved] or call [JmDNS.getServiceInfo]
     *
     * <pre>
     * ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName())
    </pre> *
     *
     *
     * Please note that service resolution may take a few second to resolve.
     *
     *
     * @param event
     * The ServiceEvent providing the name and fully qualified type of the service.
     */
    suspend fun serviceAdded(event: ServiceEvent)

    /**
     * A service has been removed.
     *
     * @param event
     * The ServiceEvent providing the name and fully qualified type of the service.
     */
    suspend fun serviceRemoved(event: ServiceEvent)

    /**
     * A service has been resolved. Its details are now available in the ServiceInfo record.<br></br>
     * **Note:**This call back will never be called if the service does not resolve.<br></br>
     *
     * @param event
     * The ServiceEvent providing the name, the fully qualified type of the service, and the service info record.
     */
    suspend fun serviceResolved(event: ServiceEvent)
}