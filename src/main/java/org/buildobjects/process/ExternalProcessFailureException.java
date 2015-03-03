package org.buildobjects.process;

/**
 * Signals the failure of an external process that returned a non zero exit code. It captures additional information
 * such as the output on stderr.
 */
public class ExternalProcessFailureException extends RuntimeException {
    private String command;
    private int exitValue;
    private String stderr;
    private long time;

    ExternalProcessFailureException(String command, int exitValue, String stderr, long time) {
        this.command = command;
        this.exitValue = exitValue;
        this.stderr = stderr;
        this.time = time;
    }

    @Override
    public String getMessage() {
        return "External process '" + command + "' returned " + exitValue +" after " + time + "ms\n" + stderr ;
    }

    /** @return the command that was executed */
    public String getCommand() {
        return command;
    }

    /** @return the actual exit value */
    public int getExitValue() {
        return exitValue;
    }

    /** @return the output on stderr */
    public String getStderr() {
        return stderr;
    }

    /** @return the execution time until the process failed*/    
    public long getTime() {
        return time;
    }
}
