package com.example.consumer

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.consumer.features.orchestrator.FlutterBridge
import com.example.consumer.features.os_stats.OsReader
import com.example.consumer.features.process_manager.SlotManager

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.consumer/bridge"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val osReader = OsReader(applicationContext)
        // Pass both osReader and filesDir to SlotManager
        val slotManager = SlotManager(osReader = osReader, filesDir = filesDir)
        
        // [NEW] Manager for Isolated Service
        val workerManager = com.example.consumer.features.orchestrator.IsolatedWorkerManager(applicationContext)
        
        val flutterBridge = FlutterBridge(osReader, slotManager, workerManager)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler(flutterBridge)
    }
}
