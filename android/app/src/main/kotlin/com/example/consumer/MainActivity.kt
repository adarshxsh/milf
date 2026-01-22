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

        val osReader = OsReader()
        val slotManager = SlotManager(filesDir = filesDir)
        val flutterBridge = FlutterBridge(osReader, slotManager)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler(flutterBridge)
    }
}
