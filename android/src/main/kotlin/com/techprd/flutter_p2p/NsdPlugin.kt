package com.techprd.flutter_p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.PROTOCOL_DNS_SD
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.MethodChannel
import timber.log.Timber
import java.net.InetAddress
import java.net.ServerSocket

class NsdPlugin(mContext: Context) {

    var mLocalPort: Int = 0
    lateinit var serverSocket: ServerSocket
    var SERVICE_TYPE = "_http._tcp."
    var SERVICE_NAME = "com.techprd.NSD.service"
    var mNsdManager: NsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    var mDeviceName: String = "User"
    var mResolveListener: NsdManager.ResolveListener? = null
    var mLostResolveListener: NsdManager.ResolveListener? = null
    var mDiscoveryListener: NsdManager.DiscoveryListener? = null
    var mRegistrationListener: NsdManager.RegistrationListener? = null
    var mServiceName = "com.techprd.NSD"
    lateinit var discoverChannel: MethodChannel

    fun initializeServerSocket() {
        // Initialize a server socket on the next available port.
        serverSocket = ServerSocket(0).also { socket ->
            // Store the chosen port.
            mLocalPort = socket.localPort
        }
    }

    fun initializeNsd() {
        object : Thread() {
            override fun run() {
                try {
                    initializeResolveListener()
                } catch (ex: Exception) {
                    Timber.e(ex, "Exception in thread:on NSD init")
                }
            }
        }.start()
    }

    fun setDiscoveryChannel(dchannel: MethodChannel) {
        discoverChannel = dchannel
    }

    fun setServiceName(serviceName: String) {
        SERVICE_NAME = serviceName
    }

    fun setServiceType(type: String) {
        SERVICE_TYPE = type
    }

    fun initializeDiscoveryListener() {
        mDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.d("Service discovery success $service")
                if (service.serviceType != SERVICE_TYPE) {
                    Timber.d("Unknown Service Type: %s", service.serviceType)
                } else if (service.serviceName.contains(SERVICE_NAME)) {
                    Timber.d("Resolving service $service")
                    try {
                        mNsdManager.resolveService(service, mResolveListener)
                    } catch (e: Exception) {
                        Timber.w(e, "Cannot resolve service, service resolve in progress")
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.e("Service lost $service")
                if (service.serviceName.contains(SERVICE_NAME)) {
                    try {
                        mNsdManager.resolveService(service, mLostResolveListener)
                    } catch (e: Exception) {
                        Timber.w(e, "Cannot resolve lost service, service resolve in progress")
                    }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.i("Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code:$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code:$errorCode")
            }
        }
    }

    fun initializeResolveListener() {
        object : Thread() {
            override fun run() {
                try {
                    mResolveListener = object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Timber.e("Resolve failed$errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Timber.d("Resolve Succeeded. $serviceInfo")
                            val port: Int = serviceInfo.port
                            val host: InetAddress = serviceInfo.host
                            val foundData: HashMap<String, Any> = HashMap()
                            foundData["name"] = serviceInfo.serviceName.removePrefix("$SERVICE_NAME:")
                            foundData["host"] = host.hostAddress
                            foundData["port"] = port
                            Handler(Looper.getMainLooper()).post {
                                discoverChannel.invokeMethod("foundHost", foundData)
                            }
                        }
                    }

                    mLostResolveListener = object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Timber.e("LostResolve failed $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Timber.e("LostResolve Succeeded. $serviceInfo")
                            val port: Int = serviceInfo.port
                            val host: InetAddress = serviceInfo.host
                            val lostData: HashMap<String, Any> = HashMap()
                            lostData["name"] = serviceInfo.serviceName.removePrefix("$SERVICE_NAME:")
                            lostData["host"] = host.hostAddress
                            lostData["port"] = port
                            Handler(Looper.getMainLooper()).post {
                                discoverChannel.invokeMethod("lostHost", lostData)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Timber.e("Exception in thread:on NSD init ResolveListener")
                }
            }
        }.start()
    }

    fun initializeRegistrationListener() {
        mRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                mServiceName = NsdServiceInfo.serviceName
                Timber.d("Service registered: $mServiceName")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Timber.d("Service registration failed: $arg1")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Timber.d("Service unregistered: %s", arg0.serviceName)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.d("Service unregistration failed: $errorCode")
            }
        }
    }

    fun setDeviceName(name: String) {
        mDeviceName = name
    }

    fun registerService() {
        object : Thread() {
            override fun run() {
                try {
                    tearDown() // Cancel any previous registration request
                    initializeRegistrationListener()
                    val serviceInfo = NsdServiceInfo().apply {
                        port = mLocalPort
                        serviceName = "$SERVICE_NAME:$mDeviceName"
                        serviceType = SERVICE_TYPE
                    }
                    mNsdManager.registerService(serviceInfo, PROTOCOL_DNS_SD, mRegistrationListener)
                } catch (ex: Exception) {
                    Timber.e(ex, "Exception in thread:Register NSD host")
                }
            }
        }.start()
    }

    fun discoverServices() {
        object : Thread() {
            override fun run() {
                try {
                    stopDiscovery() // Cancel any existing discovery request
                    initializeDiscoveryListener()
                    mNsdManager.discoverServices(SERVICE_TYPE, PROTOCOL_DNS_SD, mDiscoveryListener)
                } catch (ex: Exception) {
                    Timber.i("Exception in thread:on NSD Start discovery")
                }
            }
        }.start()
    }

    fun stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            } finally {
            }
            mDiscoveryListener = null
        }
    }

    fun tearDown() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener)
            } finally {
            }
            mRegistrationListener = null
        }
    }
}