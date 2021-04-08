package org.buildobjects.process;

import java.io.*;
import java.util.*;

import static java.util.Arrays.asList;
import static org.buildobjects.process.Helper.asSet;


/** A builder to construct a new process. The process gets configured by the withXXX-methods and
 * spawned by the run() method*/
public class ProcBuilder {

    private ByteArrayOutputStream defaultStdout = new ByteArrayOutputStream();

    private String command;
    private List<String> args = new ArrayList<String>();
    private Map<String, String> env = new HashMap<String, String>();

    private OutputStream stdout = defaultStdout;
    private InputStream stdin;
    private OutputStream stderr;

    private Long timoutMillis = 5000L;

    private Set<Integer> expectedExitStatuses = new HashSet<Integer>(){{add(0);}};

    private File directory;

    private StreamConsumer outputConsumer;
    private StreamConsumer errorConsumer;


    /** Creates a new ProcBuilder
     * @param command The command to run
     * @param args The command line arguments
     */

    public ProcBuilder(String command, String... args) {
        this.command = command;
        withArgs(args);
    }

    /**
     * Adds another argument
     * @param arg to add
     * @return this, for chaining
     * */
    public ProcBuilder withArg(String arg) {
        args.add(arg);
        return this;

    }

    /** Redirecting the standard output. If it is not redirected the output gets captured in memory and
     * is available on the @see ProcResult
     *
     * @param stdout stream to redirect the output to. \
     * @return this, for chaining
     * */
    public ProcBuilder withOutputStream(OutputStream stdout) {
        this.stdout = stdout;
        return this;
    }

    /** Redirecting the error output. If it is not redirected the output gets captured in memory and
     * is available on the @see ProcResult
     *
     * @param stderr stream to redirect the output to. \
     * @return this, for chaining
     * */
    public ProcBuilder withErrorStream(OutputStream stderr) {
        this.stderr = stderr;
        return this;
    }


    /** Specify a timeout for the operation. If not specified the default is 5 seconds.
     * @param timeoutMillis time that the process gets to run
     * @return this, for chaining
     * */
    public ProcBuilder withTimeoutMillis(long timeoutMillis) {
        this.timoutMillis = timeoutMillis;
        return this;
    }

    /** Disable timeout for the operation.
     *
     * @return this, for chaining
     * */
    public ProcBuilder withNoTimeout() {
        this.timoutMillis = null;
        return this;
    }


    /** Take the input for the program from a given InputStream
     * @param stdin stream to read the input from
     * @return this, for chaining
     */
    public ProcBuilder withInputStream(InputStream stdin) {
        this.stdin = stdin;
        return this;
    }

    /** Supply the input as string
     * @param input the actual input
     * @return this, for chaining
     */
    public ProcBuilder withInput(String input) {
        stdin = new ByteArrayInputStream(input.getBytes());
        return this;
    }


    /** Supply the input as byte[]
     * @param input the actual input
     * @return this, for chaining
     */
    public ProcBuilder withInput(byte[] input) {
        stdin = new ByteArrayInputStream(input);
        return this;
    }

    /** Override the wokring directory
     * @param directory the working directory for the process
     * @return this, for chaining
     */
    public ProcBuilder withWorkingDirectory(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("File '" + directory.getPath() + "' is not a directory.");
        }
        this.directory = directory;
        return this;
    }


    /** Add multiple args
     *   @param args the arguments add
     *   @return this, for chaining
     */
    public ProcBuilder withArgs(String... args) {
        this.args.addAll(asList(args));
        return this;
    }

    /** Define the valid exit status codes for the command
     *
     * @param exitstatuses array containing the exit codes that are valid
     * @return the ProcBuilder object; permits chaining.
     * @author Mark Galbraith (mark.galbraith@citrix.com)
     *
     * @deprecated Please use the variants with a set or vargs parameters*/

    public ProcBuilder withExitStatuses(int[] exitstatuses) {
        this.expectedExitStatuses = asSet(exitstatuses);
        return this;
    }

    /** Define the valid exit status codes for the command
     *
     * @param expectedExitStatuses array containing the exit codes that are valid
     * @return the ProcBuilder object; permits chaining.
     * @author Mark Galbraith (mark.galbraith@citrix.com)
     */
    public ProcBuilder withExpectedExitStatuses(Set<Integer> expectedExitStatuses) {
        this.expectedExitStatuses = expectedExitStatuses;
        return this;
    }


    /** Define the valid exit status codes for the command
     *  Convenience method taking varargs.
     *
     * @param expectedExitStatuses varargs parameter containing the exit codes that are valid
     * @return the ProcBuilder object; permits chaining.
     * @author Mark Galbraith (mark.galbraith@citrix.com)
     */
    public ProcBuilder withExpectedExitStatuses(int... expectedExitStatuses) {
        this.expectedExitStatuses = asSet(expectedExitStatuses);
        return this;
    }

    /** Ignore the error status returned from this command
     *
     * @return the ProcBuilder object; permits chaining.
     */
    public ProcBuilder ignoreExitStatus() {
        this.expectedExitStatuses = Collections.emptySet();
        return this;
    }

    /** Spawn the actual execution.
     *  This will block until the process terminates.
     * @return the result of the successful execution
     * @throws StartupException if the process can't be started
     * @throws TimeoutException if the timeout kicked in
     * @throws ExternalProcessFailureException if the external process returned a non-null exit value*/
    public ProcResult run() throws StartupException, TimeoutException, ExternalProcessFailureException {

        if (stdout != defaultStdout && outputConsumer != null) {
            throw new IllegalArgumentException("You can either ...");
        }

        try {
            Proc proc = new Proc(command, args, env, stdin, outputConsumer != null ? outputConsumer : stdout , directory, timoutMillis, errorConsumer != null ? errorConsumer : stderr);

            final ByteArrayOutputStream output = defaultStdout == stdout && outputConsumer == null ? defaultStdout : null;

            if (expectedExitStatuses.size() > 0 && !expectedExitStatuses.contains(proc.getExitValue())) {
                throw new ExternalProcessFailureException(command, proc.toString(), proc.getExitValue(), proc.getErrorString(), output, proc.getExecutionTime());
            }

            return new ProcResult(proc.toString(), output, proc.getExitValue(), proc.getExecutionTime(), proc.getErrorBytes());
        } finally {
            stdout = defaultStdout = new ByteArrayOutputStream();
            stdin = null;
        }
    }

    /** Static helper to run a process
     * @param cmd the command
     * @param args the arguments
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
     * @param args the arguments
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
     * @param value the value to be passed in
     * @return this, for chaining*/
    public ProcBuilder withVar(String var, String value) {
        env.put(var, value);
        return this;
    }

    /**
     * Process the standard output with the given consumer object
     *
     * @param outputConsumer an object that defines how to process the standard output stream
     * @return this, for chaining
     */
    public ProcBuilder withOutputConsumer(StreamConsumer outputConsumer) {
        this.outputConsumer = outputConsumer;

        return this;
    }

    /**
     * Process the error output with given consumer object
     * @param errorConsumer an object that defines how to process the error output stream
     * @return this, for chaining
     */
    public ProcBuilder withErrorConsumer(StreamConsumer errorConsumer) {
        this.errorConsumer = errorConsumer;
        return this;
    }

    /** @return  a string representation of the process invocation.
     *
     *           This approximates the representation of this invocation
     *           in a shell. Note that the escaping of arguments is incomplete,
     *           it works only for whitespace. Fancy control characters are
     *           not replaced.
     *
     *           Also, this returns a representation of the current state of
     *           the builder. If more arguments are added the process this
     *           representation will not represent the process that gets launched.
     *
     * @deprecated Use getCommandLine instead.
     */
    @Deprecated
    public String getProcString() {
        return Proc.formatCommandLine(command, args);
    }

    /** @return  a string representation of the process invocation.
     *
     *           This approximates the representation of this invocation
     *           in a shell. Note that the escaping of arguments is incomplete,
     *           it works only for whitespace. Fancy control characters are
     *           not replaced.
     *
     *           Also, this returns a representation of the current state of
     *           the builder. If more arguments are added the process this
     *           representation will not represent the process that gets launched.
     */
    public String getCommandLine() {
        return Proc.formatCommandLine(command, args);
    }
}
