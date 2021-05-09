import 'dart:async';
import 'package:flutter/services.dart';

class FlutterP2p {
  static const MethodChannel _channel =
      const MethodChannel('com.techprd/flutter_nsd');
  static const MethodChannel _discoveryChannel =
      const MethodChannel('com.techprd.NSD.discovery');

  static final FlutterP2p _instance = FlutterP2p._internal();

  final _streamController = StreamController<NsdServiceInfo>();
  late Stream<NsdServiceInfo> _stream;

  Stream<NsdServiceInfo> get stream => _stream;

  FlutterP2p._internal() {
    this._stream = _streamController.stream.asBroadcastStream();
  }

  factory FlutterP2p() {
    return _instance;
  }

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future get initNSD async {
    await _channel.invokeMethod('initNSD');
  }

  static Future<void> get stopNSD async {
    await _channel.invokeMethod('stopNSD');
  }

  static Future<int> get startNSDBroadcasting async {
    var socketPort = await _channel.invokeMethod('startNSDBroadcasting');
    return socketPort;
  }

  static Future get stopNSDBroadcasting async {
    await _channel.invokeMethod("stopNSDBroadcasting");
  }

  Future<void> get searchForLocalDevices async {
    _discoveryChannel.setMethodCallHandler((call) => discoveryHandler(call));
    await _channel.invokeMethod('searchForLocalDevices');
  }

  Future<void> discoveryHandler(MethodCall call) async {
    switch (call.method) {
      case "foundHost":
        var host = NsdServiceInfo(call.arguments["host"],
            call.arguments["port"], call.arguments["name"]);
        print(host);
        _streamController.add(host);
        break;
      case "lostHost":
        var host = NsdServiceInfo(call.arguments["host"],
            call.arguments["port"], call.arguments["name"]);
        print("lost host: $host");
        break;
    }
  }

  static Future get stopSearch async {
    await _channel.invokeMethod('stopSearch');
  }
}

/// Info class for holding discovered service
class NsdServiceInfo {
  final String? host;
  final int? port;
  final String? name;

  //final Map<String, Uint8List>? txt;

  NsdServiceInfo(this.host, this.port, this.name);
}

/// List of possible error codes of NsdError
enum NsdErrorCode {
  startDiscoveryFailed,
  stopDiscoveryFailed,
  onResolveFailed,
  discoveryStopped,
}

// Generic error thrown when an error has occurred during discovery
class NsdError extends Error {
  /// The cause of this [NsdError].
  final NsdErrorCode errorCode;

  NsdError({required this.errorCode});
}
