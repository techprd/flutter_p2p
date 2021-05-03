import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class FlutterP2p {
  static const MethodChannel _channel =
      const MethodChannel('com.techprd/flutter_nsd');

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

  static Future<bool> get initNSD async {
    return await _channel.invokeMethod('initNSD');
  }

  static Future<Map<String, String>> get startNSDBroadcasting async {
    return await _channel.invokeMethod('startNSDBroadcasting');
  }

  static Future<Map<String, String>> get searchForLocalDecvices async {
    return await _channel.invokeMethod('searchForLocalDecvices');
  }
}

/// Info class for holding discovered service
class NsdServiceInfo {
  final String? hostname;
  final int? port;
  final String? name;
  final Map<String, Uint8List>? txt;

  NsdServiceInfo(this.hostname, this.port, this.name, this.txt);
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
