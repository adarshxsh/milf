import 'package:flutter_dotenv/flutter_dotenv.dart';

class Env {
  static String get cloudApiEndpoint => dotenv.env['CLOUD_API_ENDPOINT'] ?? '';
  static String get apiKey => dotenv.env['API_KEY'] ?? '';

  static Future<void> init() async {
    await dotenv.load(fileName: ".env");
  }
}
