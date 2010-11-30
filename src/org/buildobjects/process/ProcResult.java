package org.buildobjects.process;

import java.io.ByteArrayOutputStream;

/**
 * User: fleipold
 * Date: Nov 26, 2010
 * Time: 3:41:15 PM
 */
public class ProcResult {

    private final String procString;
    private final ByteArrayOutputStream output;
    private final int exitValue;
    private final long executionTime;

    public ProcResult(String procString, ByteArrayOutputStream output, int exitValue, long executionTime) {
        this.procString = procString;
        this.output = output;
        this.exitValue = exitValue;
        this.executionTime = executionTime;
    }

    public String getProcString() {
        return procString;
    }

    private ByteArrayOutputStream getOutputStream() {
        if (output == null){
            throw new IllegalStateException("Output has been consumed by client provided OutputStream");
        }
        return output;
    }


     public String getOutputString(){
        return getOutputStream().toString();
    }
    
    public byte[] getOutputBytes(){
        return getOutputStream().toByteArray();
    }

    public int getExitValue() {
        return exitValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
