package org.buildobjects.process;

import java.io.*;
import java.util.*;

import static java.util.Arrays.asList;


/** A builder to construct a new process. The process gets configured by the withXXX-methods and
 * spawned by the run() method*/
public class ProcBuilder {

    ByteArrayOutputStream defaultStdout = new ByteArrayOutputStream();

    String command;
    List<String> args = new ArrayList<String>();
    private Map<String, String> env = new HashMap<String, String>();

    OutputStream stdout = defaultStdout;
    InputStream stdin;

    long timoutMillis = 5000;
    
    int[] expectedExitStatuses = {0};

    File directory;


    /** Creates a new ProcBuilder
     * @param command The command to run
     * @param args The command line arguments*/
    public ProcBuilder(String command, String... args) {
        this.command = command;
        withArgs(args);
    }

    /**
     * Adds another argument
     * @param arg to add */
    public ProcBuilder withArg(String arg){
        args.add(arg);
        return this;

    }

    /** Redirecting the standard output. If it is not redirected the output gets captured in memory and
     * is available on the @see ProcResult
     * 
     * @param stdout stream to redirect the output to. */
    public ProcBuilder withOutputStream(OutputStream stdout){
        this.stdout = stdout;
        return this;
    }


    /** Specify a timeout for the operation. If not specified the default is 5secs
     * @param timeoutMillis*/
    public ProcBuilder withTimeoutMillis(long timeoutMillis){
        this.timoutMillis = timeoutMillis;
        return this;
    }


    /** Take the input for the program from a given InputStream
     * @param stdin stream to read the input from*/
    public ProcBuilder withInputStream(InputStream stdin){
        this.stdin = stdin;
        return this;
    }

    /** Supply the input as string
     * @param input the actual input*/
    ProcBuilder withInput(String input){
        stdin = new ByteArrayInputStream(input.getBytes());
        return this;
    }


    /** Supply the input as byte[]
     * @param input the actual input*/
    ProcBuilder withInput(byte[] input){
        stdin = new ByteArrayInputStream(input);
        return this;
    }

    /** Override the wokring directory
     * @param directory the working directory for the process*/
    public ProcBuilder withWorkingDirectory(File directory){
        if (!directory.isDirectory()){
            throw new IllegalArgumentException("File '" + directory.getPath() + "' is not a directory.");
        }
        this.directory = directory;
        return this;
    }



    /** Add multiple args
     *   @param args the arguments add*/
    public ProcBuilder withArgs(String... args) {
        this.args.addAll(asList(args));
        return this;
    }
    
    /** Define the valid exit status codes for the command
     * 
     * @param exitcodes the array containing the exit codes that are valid
     * @return the ProcBuilder object; permits chaining.
     * @author Mark Galbraith (mark.galbraith@citrix.com)
     */
    public ProcBuilder withExitStatuses(int[] exitstatuses) {
    	this.expectedExitStatuses = exitstatuses;
    	return this;
    }
    
    /** Ignore the error status returned from this command
     * 
     * @return the ProcBuilder object; permits chaining.
     */
    public ProcBuilder ignoreExitStatus() {
    	int[] emptySet = {};
    	this.expectedExitStatuses = emptySet;
    	return this;
    }

    /** Spawn the actual execution.
     *  This will block until the process terminates.
     * @return the result of the successful execution
     * @throws StartupException if the process can't be started
     * @throws TimeoutException if the timeout kicked in
     * @throws ExternalProcessFailureException if the external process returned a non-null exit value*/
    public ProcResult run() throws StartupException, TimeoutException, ExternalProcessFailureException{
        try{
            Proc proc = new Proc(command, args, env, stdin, stdout, directory, timoutMillis, expectedExitStatuses);

            return new ProcResult(proc.toString(), defaultStdout == stdout ? defaultStdout : null, proc.getExitValue(), proc.getExecutionTime());
        }
        finally {
            defaultStdout = new ByteArrayOutputStream();
            stdin = null;
            stdout = defaultStdout;
        }
    }
   

    /** Static helper to run a process
     * @param cmd the command
     * @param args the argmuments
     * @return the standard output
     * @throws StartupException if the process can't be started
     * @throws TimeoutException if the timeout kicked in
     * @throws ExternalProcessFailureException if the external process returned a non-null exit value
     *  */
    public static String run(String cmd, String... args) {
        ProcBuilder builder= new ProcBuilder(cmd)
                .withArgs(args);

        return builder.run().getOutputString();
    }

    /** Static helper to filter a string through a process
     * @param input the input to be fed into the process
     * @param cmd the command
     * @param args the argmuments
     * @return the standard output
     * @throws StartupException if the process can't be started
     * @throws TimeoutException if the timeout kicked in
     * @throws ExternalProcessFailureException if the external process returned a non-null exit value
     *  */
    public static String filter(String input, String cmd, String... args) {
        ProcBuilder builder= new ProcBuilder(cmd)
                .withArgs(args)
                .withInput(input);


        return builder.run().getOutputString();
    }

    /** Add a variable to the processes environment
     * @param var variable name
     * @param value the value to be passed in*/
    public ProcBuilder withVar(String var, String value) {
        env.put(var, value);
        return this;
    }
}
