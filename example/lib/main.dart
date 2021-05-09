import 'package:flutter/material.dart';

import 'package:flutter_p2p/flutter_p2p.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformResponse = 'Unknown';
  FlutterP2p flutterP2p = FlutterP2p();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    var clients = [];
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: SingleChildScrollView(
          physics: ScrollPhysics(),
          child: Column(
            children: [
              ElevatedButton(
                  onPressed: () async {
                    FlutterP2p.initNSD;
                    var port = await FlutterP2p.startNSDBroadcasting;
                    setState(() {
                      _platformResponse = "running on port: $port";
                    });
                  },
                  child: Text("start Broadcasting")),
              ElevatedButton(
                  onPressed: () async {
                    FlutterP2p.stopNSDBroadcasting;
                    FlutterP2p.stopNSD;
                    setState(() {
                      _platformResponse = "not running";
                    });
                  },
                  child: Text("stop Broadcasting")),
              ElevatedButton(
                  onPressed: () async {
                    await flutterP2p.searchForLocalDevices;
                  },
                  child: Text("start discovery")),
              ElevatedButton(
                  onPressed: () async {
                    await FlutterP2p.stopSearch;
                  },
                  child: Text("stop discovery")),
              Text(_platformResponse),
              Divider(),
              StreamBuilder<NsdServiceInfo>(
                  stream: flutterP2p.stream,
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      clients.add(snapshot.data);
                      return ListView.builder(
                          physics: NeverScrollableScrollPhysics(),
                          shrinkWrap: true,
                          itemCount: clients.length,
                          itemBuilder: (context, index) {
                            return Card(
                              child: ListTile(
                                leading: Icon(Icons.phone_android),
                                title: Text(clients[index].name),
                                subtitle: Text(clients[index].host +
                                    " : " +
                                    clients[index].port.toString()),
                              ),
                            );
                          });
                    }
                    return Center(
                        child: SizedBox(
                            width: 50.0,
                            height: 50.0,
                            child: const CircularProgressIndicator()));
                  })
            ],
          ),
        ),
      ),
    );
  }
}
