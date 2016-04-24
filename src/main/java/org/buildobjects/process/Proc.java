package org.buildobjects.process;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Internal implementation of the process mechanics
 */
class Proc {

    private Process process;
    private int exitValue;

    private long executionTime;

    private ByteArrayOutputStream err = new ByteArrayOutputStream();
    private String command;
    private List<String> args;

    public Proc(String command,
                List<String> args,
                Map<String, String> env,
                InputStream stdin,
                OutputStream stdout,
                File directory,
                long timeout)
            throws StartupException, TimeoutException, ExternalProcessFailureException {
    	int[] exitstatuses = {0};  // Set the default for the old behavior
    	// Call the _Proc helper
    	_Proc(command, args, env, stdin, stdout, directory, timeout, exitstatuses);
    }
    
    public Proc(String command,
            	List<String> args,
            	Map<String, String> env,
            	InputStream stdin,
            	OutputStream stdout,
            	File directory,
            	long timeout,
            	int[] exitstatuses)
            throws StartupException, TimeoutException, ExternalProcessFailureException {
    	// Call the _Proc helper
    	_Proc(command, args, env, stdin, stdout, directory, timeout, exitstatuses);
    }
    
    public Proc _Proc(String command,
            List<String> args,
            Map<String, String> env,
            InputStream stdin,
            OutputStream stdout,
            File directory,
            long timeout,
            int[] exitstatuses)
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


            boolean success = done.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (!success) {
                process.destroy();
                ioHandler.cancelConsumption();
                throw new TimeoutException(toString(), timeout);
            }

            ioHandler.joinConsumption();

             
            executionTime = System.currentTimeMillis() - t1;
            // Check for accepted exit codes
            Boolean validExitCode = false;
            if (exitstatuses.length > 0) {
            	for (int i=0; i < exitstatuses.length; i++){
            		if (exitstatuses[i] == exitValue) {
            			validExitCode = true;
            		}
            	}
            } else {
            	validExitCode = true;
            }
            if (! validExitCode) {
                throw new ExternalProcessFailureException(toString(), exitValue, err.toString(), executionTime);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }
        return this;
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
            if (stringIterator.hasNext()){
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
