import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'config/env.dart';
import 'config/service_locator.dart';
import 'modules/native_bridge/native_bridge_module.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  bool envLoaded = false;
  try {
    await Env.init();
    envLoaded = true;
  } catch (e) {
    debugPrint("Warning: Failed to load .env file: $e");
  }

  await setupServiceLocator();

  runApp(ConsumerApp(envLoaded: envLoaded));
}

class ConsumerApp extends StatelessWidget {
  final bool envLoaded;
  const ConsumerApp({super.key, required this.envLoaded});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Consumer Node',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blueGrey),
        useMaterial3: true,
      ),
      home: ConsumerHomePage(envLoaded: envLoaded),
    );
  }
}

class ConsumerHomePage extends StatefulWidget {
  final bool envLoaded;
  const ConsumerHomePage({super.key, required this.envLoaded});

  @override
  State<ConsumerHomePage> createState() => _ConsumerHomePageState();
}

class _ConsumerHomePageState extends State<ConsumerHomePage> {
  String? _lastResultPath;
  String? _lastResultContent;
  bool _isRunning = false;
  final NativeBridge _bridge = NativeBridge();

  Future<void> _runWasmTest() async {
    setState(() {
      _isRunning = true;
      _lastResultContent = null;
    });

    // Dummy WASM binary
    final dummyWasm = Uint8List.fromList([
      0x00,
      0x61,
      0x73,
      0x6d,
      0x01,
      0x00,
      0x00,
      0x00,
    ]);
    final result = await _bridge.runWasmTest(dummyWasm, {
      "test_id": "123",
      "action": "run_sim",
      "user": "developer",
    });

    setState(() {
      _lastResultPath = result?['path'];
      _lastResultContent = result?['content'];
      _isRunning = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Consumer Node')),
      body: SingleChildScrollView(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(height: 32),
              const Icon(Icons.cloud_sync, size: 64, color: Colors.blueGrey),
              const SizedBox(height: 16),
              const Text('Node is Active', style: TextStyle(fontSize: 24)),
              const SizedBox(height: 8),
              Text(
                'Environment: ${widget.envLoaded ? "Loaded" : "Missing .env"}',
              ),
              const SizedBox(height: 32),
              if (_isRunning)
                const CircularProgressIndicator()
              else
                ElevatedButton(
                  onPressed: _runWasmTest,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blueGrey,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 32,
                      vertical: 16,
                    ),
                  ),
                  child: const Text('Run Test WASM'),
                ),
              if (_lastResultContent != null) ...[
                const SizedBox(height: 32),
                const Text(
                  'Execution Output:',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(height: 12),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.black87,
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.1),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Text(
                      _lastResultContent!,
                      style: const TextStyle(
                        color: Colors.greenAccent,
                        fontFamily: 'monospace',
                        fontSize: 13,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                const Text(
                  'Stored At:',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
                const SizedBox(height: 8),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 40),
                  child: Text(
                    _lastResultPath ?? '',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 10,
                      color: Colors.grey,
                      fontFamily: 'monospace',
                    ),
                  ),
                ),
                const SizedBox(height: 32),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
