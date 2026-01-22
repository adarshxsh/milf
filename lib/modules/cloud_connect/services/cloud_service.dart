class CloudService {
  Future<bool> receiveJob(
    String jobId,
    String funcId,
    List<int> wasmBinary,
    Map<String, dynamic> inputPayload,
  ) async {
    // TODO: Implement
    return true;
  }

  Future<void> heartBeat() async {
    // TODO: Implement
  }

  Future<void> jobStatus(String jobId) async {
    // TODO: Implement
  }

  Future<void> postResult() async {
    // TODO: Implement
  }

  Future<void> initiateProcess(
    String funcId,
    List<int> wasmBinary,
    Map<String, dynamic> inputPayload,
    Map<String, dynamic> metadata,
  ) async {
    // TODO: Contact kotlin connect subunit
  }
}
