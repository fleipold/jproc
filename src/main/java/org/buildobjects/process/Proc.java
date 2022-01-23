package org.buildobjects.process;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.buildobjects.process.ExecutionEvent.EXCEPTION_IN_STREAM_HANDLING;
import static org.buildobjects.process.ExecutionEvent.PROCESS_EXITED;


/**
 * Internal implementation of the process mechanics
 */
class Proc implements EventSink {

    private final Process process;
    private int exitValue;

    private long executionTime;

    private final OutputConsumptionThread err;
    private final String command;
    private final List<String> args;
    private final Long timeout;
    private final BlockingQueue<ExecutionEvent> eventQueue = new LinkedBlockingQueue<ExecutionEvent>();
    private final IoHandler ioHandler;

    public Proc(String command,
                List<String> args,
                Map<String, String> env,
                boolean clearEnvironment,
                InputStream stdin,
                Object stdout,
                File directory,
                Long timeout,
                Object stderr)
            throws StartupException, TimeoutException, ExternalProcessFailureException {

        this.command = command;
        this.args = args;
        this.timeout = timeout;
        String[] cmdArray = concatenateCmdArgs();
        long t1 = System.currentTimeMillis();

        OutputConsumptionThread stdoutConsumer;

        try {
            ProcessBuilder builder = new ProcessBuilder(cmdArray)
                    .directory(directory);

            if (clearEnvironment) {
                builder.environment().clear();
            }

            builder.environment().putAll(env);
            process = builder.start();

            stdoutConsumer = createStreamConsumer(stdout);

            if (stderr == null) {
                err = new ByteArrayConsumptionThread(this);
            } else {
                err = createStreamConsumer(stderr);
            }


            ioHandler = new IoHandler(stdin, stdoutConsumer, err, process);

        } catch (IOException e) {
            throw new StartupException("Could not startup process '" + toString() + "'.", e);
        }

        try {
            startControlThread();

            do {
                ExecutionEvent nextEvent = timeout == null ? eventQueue.poll(MAX_VALUE, HOURS) : eventQueue.poll(timeout, MILLISECONDS);

                if (nextEvent == null) {
                    killCleanUpAndThrowTimeoutException();
                }

                if (nextEvent == PROCESS_EXITED) {
                    break;
                }

                if (nextEvent == EXCEPTION_IN_STREAM_HANDLING) {
                    killProcessCleanup();
                    break;
                }

                throw new RuntimeException("Felix reckons we should never reach this point");
            } while (true);

            List<Throwable> exceptions = ioHandler.joinConsumption();
            if (!exceptions.isEmpty()) {
                throw new IllegalStateException("Exception in stream consumption", exceptions.get(0));
            }

            executionTime = System.currentTimeMillis() - t1;


        } catch (InterruptedException e) {
            killProcessCleanup();
            throw new RuntimeException("Control Thread was interrupted killed process.", e);
        }
    }

    private OutputConsumptionThread createStreamConsumer(Object stream) {
        if (stream instanceof OutputStream) {
            return new StreamCopyConsumptionThread((OutputStream) stream, this);
        } else if (stream instanceof StreamConsumer) {
            return new StreamConsumerConsumptionThread(Proc.this, (StreamConsumer) stream);
        } else {
            throw new RuntimeException("Badness, badness");
        }
    }

    byte[] getErrorBytes() {
        if (err instanceof ByteArrayConsumptionThread) {
            return ((ByteArrayConsumptionThread) err).getBytes();
        }
        // Output stream/stream consumer was provided by user, we don't own it.
        return null;
    }

    public String getErrorString() {
        byte[] bytes = getErrorBytes();
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    private void startControlThread() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    exitValue = process.waitFor();
                    dispatch(PROCESS_EXITED);
                } catch (InterruptedException e) {
                    throw new RuntimeException("", e);
                }
            }
        }).start();
    }

    private void killCleanUpAndThrowTimeoutException() {
        process.destroy();
        ioHandler.cancelConsumption();
        throw new TimeoutException(toString(), timeout);
    }

    private void killProcessCleanup() {
        process.destroy();
        ioHandler.cancelConsumption();
    }

    public void dispatch(ExecutionEvent event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            throw new RuntimeException("${END}", e);
        }
    }

    private String[] concatenateCmdArgs() {
        List<String> cmd = new ArrayList<String>();
        cmd.add(command);
        cmd.addAll(args);
        return cmd.toArray(new String[cmd.size()]);
    }


    @Override
    public String toString() {
        return formatCommandLine(command, args);
    }

    static String formatCommandLine(String command, List<String> args) {
        return command + " " + argsString(args);
    }

    private static String argsString(List<String> args) {
        StringBuffer temp = new StringBuffer();
        for (Iterator<String> stringIterator = args.iterator(); stringIterator.hasNext(); ) {
            String arg = stringIterator.next();
            String escapedArg = naiveShellEscape(arg);
            temp.append(escapedArg);
            if (stringIterator.hasNext()) {
                temp.append(" ");
            }
        }
        return temp.toString();
    }

    private static String naiveShellEscape(String arg) {
        Pattern p = Pattern.compile("\\s");
        Matcher m = p.matcher(arg);
        String escapedArg;
        if (m.find()) {
            escapedArg = "'" + arg.replaceAll("'", "'\"'\"'") + "'";
        } else {
            escapedArg = arg;
        }
        return escapedArg;
    }

    public int getExitValue() {
        return exitValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
