package org.buildobjects.process;

/**
 * User: fleipold
 * Date: Nov 25, 2010
 * Time: 10:30:53 PM
 */
public class ExternalProcessFailureException extends RuntimeException {
    private String command;
    private int exitValue;
    private String stderr;
    private long time;

    public ExternalProcessFailureException(String command, int exitValue, String stderr, long time) {
        this.command = command;
        this.exitValue = exitValue;
        this.stderr = stderr;
        this.time = time;
    }

    @Override
    public String getMessage() {
        return "External process '" + command + "' returned " + exitValue +" after " + time + "ms\n" + stderr ;
    }

    public String getCommand() {
        return command;
    }

    public int getExitValue() {
        return exitValue;
    }

    public String getStderr() {
        return stderr;
    }

    public long getTime() {
        return time;
    }
}
