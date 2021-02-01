package org.buildobjects.process;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Represents the result of a successful process execution.
 */
public class ProcResult {

    private final String procString;
    private final ByteArrayOutputStream output;
    private final int exitValue;
    private final long executionTime;
    private final byte[] err;


    ProcResult(String procString, ByteArrayOutputStream output, int exitValue, long executionTime, byte[] err) {
        this.procString = procString;
        this.output = output;
        this.exitValue = exitValue;
        this.executionTime = executionTime;
        this.err = err != null ? Arrays.copyOf(err, err.length) : null;
    }

    /** @return  a string representation of the process invocation.
     *
     *           This approximates the representation of this invocation
     *           in a shell. Note that the escaping of arguments is incomplete,
     *           it works only for whitespace. Fancy control characters are
     *           not replaced.
     */
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

    /** @return the standard error as string
     *  @throws IllegalStateException if an OutputStream has been provided to capture the error output */
    public String getErrorString() throws IllegalStateException {
        return new String(getErrorBytes());
    }

    /** @return the standard error as byte[]
     *  @throws IllegalStateException if an OutputStream has been provided to capture the error output */
    public byte[] getErrorBytes() throws IllegalStateException {
        if(err == null) {
            throw new IllegalStateException("Error output has been consumed by client provided OutputStream");
        }
        return err; // Should we make defensive copy?
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
