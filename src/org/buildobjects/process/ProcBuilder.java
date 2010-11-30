package org.buildobjects.process;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;


public class ProcBuilder {

    ByteArrayOutputStream defaultStdout = new ByteArrayOutputStream();

    String command;
    List<String> args = new ArrayList<String>();
    private Map<String, String> env = new HashMap<String, String>();

    OutputStream stdout = defaultStdout;
    InputStream stdin;

    long timoutMillis = 5000;

    File directory;


    public ProcBuilder(String command, String... args) {
        this.command = command;
        withArgs(args);
    }

    public ProcBuilder withArg(String arg){
        args.add(arg);
        return this;

    }

    public ProcBuilder withOutputStream(OutputStream stdout){
        this.stdout = stdout;
        return this;
    }


    public ProcBuilder withTimeoutMillis(long timeoutMillis){
        this.timoutMillis = timeoutMillis;
        return this;
    }


    public ProcBuilder withInputStream(InputStream stdin){
        this.stdin = stdin;
        return this;
    }

    ProcBuilder withInput(String input){
        stdin = new ByteArrayInputStream(input.getBytes());
        return this;
    }

    ProcBuilder withInput(byte[] input){
        stdin = new ByteArrayInputStream(input);
        return this;
    }

    public ProcBuilder withWorkingDirectory(File directory){
        if (!directory.isDirectory()){
            throw new IllegalArgumentException("File '" + directory.getPath() + "' is not a directory.");
        }
        this.directory = directory;
        return this;
    }



    public ProcBuilder withArgs(String... args) {
        this.args.addAll(asList(args));
        return this;
    }

    public ProcResult run(){
        try{
            Proc proc = new Proc(command, args, env, stdin, stdout, directory, timoutMillis);

            return new ProcResult(proc.toString(), defaultStdout == stdout ? defaultStdout : null, proc.getExitValue(), proc.getExecutionTime());
        }
        finally {
            defaultStdout = new ByteArrayOutputStream();
            stdin = null;
            stdout = defaultStdout;
        }
    }
   

    public static String run(String cmd, String... args) {
        ProcBuilder builder= new ProcBuilder(cmd)
                .withArgs(args);

        return builder.run().getOutputString();
    }

    public static String filter(String input, String cmd, String... args) {
        ProcBuilder builder= new ProcBuilder(cmd)
                .withArgs(args)
                .withInput(input);


        return builder.run().getOutputString();
    }

    public ProcBuilder withVar(String var, String value) {
        env.put(var, value);
        return this;
    }
}
