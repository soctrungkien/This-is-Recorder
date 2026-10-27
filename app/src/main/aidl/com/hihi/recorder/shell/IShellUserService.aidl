package com.hihi.recorder.shell;

/**
 * Runs inside the process started by Shizuku with the shell identity.
 *
 * The method IDs are fixed so that an updated app can keep talking to a
 * UserService that Shizuku started for an older version. The destroy() ID is
 * reserved by the Shizuku server.
 */
interface IShellUserService {
    void destroy() = 16777114;
    void exit() = 1;
    String runCommand(String command, int timeoutMs) = 2;
    long startSegment(String path, String size, int bitRate, int timeLimitSeconds) = 3;
    void stopSegment() = 4;
    long segmentEndedAt() = 5;
    int segmentExitCode() = 6;
    String segmentError() = 7;
    boolean segmentRunning() = 8;
}
