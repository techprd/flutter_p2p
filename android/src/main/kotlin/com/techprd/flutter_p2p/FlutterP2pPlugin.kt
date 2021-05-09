package com.techprd.flutter_p2p

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import timber.log.Timber

/** FlutterP2pPlugin */
class FlutterP2pPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var discoveryChannel: MethodChannel
    private var mNsdPlugin: NsdPlugin? = null
    private lateinit var mainHandler: Handler
    private lateinit var mContext: Context
    private var isDiscovery: Boolean = false

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mContext = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.techprd/flutter_nsd")
        discoveryChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.techprd.NSD.discovery")
        channel.setMethodCallHandler(this)
        mainHandler = Handler(Looper.getMainLooper())
        Timber.d("Plugin initialized successfully")
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                Timber.d("getPlatformVersion")
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "initNSD" -> {
                mNsdPlugin = NsdPlugin(mContext)
                mNsdPlugin?.initializeNsd()
            }
            "stopNSD" -> {
                mNsdPlugin?.tearDown()
                isDiscovery = false
                mNsdPlugin = null
            }
            "startNSDBroadcasting" -> {
                call.argument<String>("serviceName")?.let { mNsdPlugin?.setServiceName(serviceName = it) }
                call.argument<String>("serviceType")?.let { mNsdPlugin?.setServiceType(type = it) }
                mNsdPlugin?.initializeServerSocket()
                mNsdPlugin?.registerService()
                result.success(mNsdPlugin?.mLocalPort)
            }
            "stopNSDBroadcasting" -> {
                mNsdPlugin?.tearDown()
            }
            "searchForLocalDevices" -> {
                isDiscovery = true
                mNsdPlugin?.setDiscoveryChannel(discoveryChannel)
                call.argument<String>("serviceName")?.let { mNsdPlugin?.setServiceName(serviceName = it) }
                call.argument<String>("serviceType")?.let { mNsdPlugin?.setServiceType(type = it) }
                mNsdPlugin?.discoverServices()
            }
            "stopSearch" -> {
                isDiscovery = false
                mNsdPlugin?.stopDiscovery()
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        if (isDiscovery) mNsdPlugin?.stopDiscovery()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Timber.plant(Timber.DebugTree())
    }

    override fun onDetachedFromActivityForConfigChanges() {
        if (!isDiscovery) {
            mNsdPlugin?.stopDiscovery()
            Timber.d("Discovery stopped on pause")
        }
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        object : Thread() {
            override fun run() {
                try {
                    if (isDiscovery) mNsdPlugin?.discoverServices()
                } catch (ex: Exception) {
                    Timber.e(ex, "Exception in thread:onResume")
                }
            }
        }.start()
    }

    override fun onDetachedFromActivity() {
        mNsdPlugin?.tearDown()
        isDiscovery = false
        mNsdPlugin = null
    }
}
