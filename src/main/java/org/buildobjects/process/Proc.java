package org.buildobjects.process;


import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Internal implementation of the process mechanics
 */
class Proc {

    private final Process process;
    private int exitValue;

    private long executionTime;

    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final String command;
    private final List<String> args;

    public Proc(String command,
                List<String> args,
                Map<String, String> env,
                InputStream stdin,
                OutputStream stdout,
                File directory,
                Long timeout)
            throws StartupException, TimeoutException, ExternalProcessFailureException {
        this(command, args, env, stdin, stdout, directory, timeout, new HashSet<Integer>());
    }

    public Proc(String command,
            List<String> args,
            Map<String, String> env,
            InputStream stdin,
            OutputStream stdout,
            File directory,
            Long timeout,
            Set<Integer> expectedExitStatuses)
        throws StartupException, TimeoutException, ExternalProcessFailureException {

        this.command = command;
        this.args = args;
        String[] envArray = getEnv(env);

        String[] cmdArray = concatenateCmdArgs();
        long t1 = System.currentTimeMillis();
        IoHandler ioHandler ;
        try {
            process = Runtime.getRuntime().exec(cmdArray, envArray, directory);


            ioHandler = new IoHandler(stdin, stdout, err, process);

        } catch (IOException e) {
            throw new StartupException("Could not startup process '" + toString() + "'.", e);
        }


        try {
            final Semaphore done = new Semaphore(1);
            done.acquire();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        exitValue = process.waitFor();
                        done.release();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("", e);
                    }
                }
            }).start();


            boolean success;
            if (timeout != null) {
                success = done.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            } else {
                done.acquire();
                success = true;
            }

            if (!success) {
                process.destroy();
                ioHandler.cancelConsumption();
                throw new TimeoutException(toString(), timeout);
            }

            ioHandler.joinConsumption();

            executionTime = System.currentTimeMillis() - t1;

            if (expectedExitStatuses.size() > 0 && !expectedExitStatuses.contains(exitValue)) {
                throw new ExternalProcessFailureException(toString(), exitValue, err.toString(), executionTime);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }
    }

    private String[] getEnv(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return null;
        }
        String[] retValue = new String[env.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : env.entrySet()) {
            retValue[i++] = entry.getKey() + "=" + entry.getValue();

        }
        return retValue;

    }

    private String[] concatenateCmdArgs() {
        List<String> cmd = new ArrayList<String>();
        cmd.add(command);
        cmd.addAll(args);
        return cmd.toArray(new String[cmd.size()]);
    }


    @Override
    public String toString() {
        return command + " " + argString();
    }

    private String argString() {
        StringBuffer temp = new StringBuffer();
        for (Iterator<String> stringIterator = args.iterator(); stringIterator.hasNext();) {
            String arg = stringIterator.next();
            if (arg.contains(" ")) {
                temp.append("\"" + arg + "\"");
            } else {
                temp.append(arg);
            }
            if (stringIterator.hasNext()) {
                temp.append(" ");
            }

        }
        return temp.toString();
    }

    public int getExitValue() {
        return exitValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
