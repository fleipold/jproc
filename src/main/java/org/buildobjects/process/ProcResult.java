package org.buildobjects.process;

import java.io.ByteArrayOutputStream;

/**
 * Represents the result of a successful process execution.
 */
public class ProcResult {

    private final String procString;
    private final ByteArrayOutputStream output;
    private final int exitValue;
    private final long executionTime;


    ProcResult(String procString, ByteArrayOutputStream output, int exitValue, long executionTime) {
        this.procString = procString;
        this.output = output;
        this.exitValue = exitValue;
        this.executionTime = executionTime;
    }

    /** @return  a string representation of the process execution */
    public String getProcString() {
        return procString;
    }


    private ByteArrayOutputStream getOutputStream() throws IllegalStateException {
        if (output == null) {
            throw new IllegalStateException("Output has been consumed by client provided OutputStream");
        }
        return output;
    }


    /** @return the standard output as string
     *  @throws IllegalStateException if an OutputStream has been provided to captured the output */
    public String getOutputString() throws IllegalStateException {
        return getOutputStream().toString();
    }


    /** @return the standard output as byte[]
     *  @throws IllegalStateException if an OutputStream has been provided to captured the output */
    public byte[] getOutputBytes() throws IllegalStateException {
        return getOutputStream().toByteArray();
    }

    /** @return the exit value of the process */
    public int getExitValue() {
        return exitValue;
    }

    /** @return the time the execution took in milliseconds. */
    public long getExecutionTime() {
        return executionTime;
    }
}
