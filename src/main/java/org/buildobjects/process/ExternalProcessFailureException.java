package org.buildobjects.process;

import java.io.ByteArrayOutputStream;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Signals the failure of an external process that returned a non zero exit code. It captures additional information
 * such as the output on stderr.
 */
public class ExternalProcessFailureException extends RuntimeException {
    final private String command;
    final private int exitValue;
    final private String stderr;
    private final ByteArrayOutputStream stdout;
    final private long time;

    ExternalProcessFailureException(String command, int exitValue, String stderr, ByteArrayOutputStream stdOut, long time) {
        this.command = command;
        this.exitValue = exitValue;
        this.stderr = stderr;
        this.stdout = stdOut;
        this.time = time;
    }

    private String prefixLines(String string, String prefix) {
        StringBuilder builder = new StringBuilder();
        String[] lines = string.split("\n");
        for (String line : lines) {
            builder.append(prefix + line + "\n");
        }
        return builder.toString();
    }

    @Override
    public String getMessage() {
        String formattedStdErr = stderr != null ? stderr : "Stderr unavailable as it has been consumed by user provided stream.";
        String formattedStdOut = stdout != null ? new String(stdout.toByteArray(), UTF_8) : "";
        if (!formattedStdErr.isEmpty() && !formattedStdOut.isEmpty()) {
            formattedStdErr = prefixLines(formattedStdErr, "STDERR: ");
            formattedStdOut = prefixLines(formattedStdOut, "STDOUT: ");
        }

        return
            "External process '" + command +
                "' returned " + exitValue +
                " after " + time + "ms\n" +
                formattedStdErr +
                formattedStdOut;

    }

    /**
     * @return the command that was executed
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return the actual exit value
     */
    public int getExitValue() {
        return exitValue;
    }

    /**
     * @return the output on stderr
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * @return the execution time until the process failed
     */
    public long getTime() {
        return time;
    }
}
