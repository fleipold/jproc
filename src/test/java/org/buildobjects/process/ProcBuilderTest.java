package org.buildobjects.process;

import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * [DOC file=README.md]
 *
 * [![Build Status](https://app.travis-ci.com/fleipold/jproc.svg?branch=master)](https://app.travis-ci.com/fleipold/jproc)
 * [![Maven Central](https://img.shields.io/maven-central/v/org.buildobjects/jproc.svg?maxAge=600)](https://mvnrepository.com/artifact/org.buildobjects/jproc)
 *
 * Intro
 * -----
 *
 * Running external commands in Java is an error prone task.
 * `JProc` helps managing input and output of non-interactive,
 * external processes as well as error conditions. It uses sensible
 * defaults, such as throwing an exception if a process terminates
 * with a non zero exit status.
 *
 * Getting Started
 * ---------------
 *
 * To get started  either download the [jar](https://oss.sonatype.org/content/repositories/releases/org/buildobjects/jproc/2/jproc-2.jar) or
 * if you are using maven add this snippet to your pom:
 *
 * ~~~ .xml
 * <dependency>
 *     <groupId>org.buildobjects</groupId>
 *     <artifactId>jproc</artifactId>
 *     <version>2.8.2</version>
 * </dependency>
 * ~~~
 */

public class ProcBuilderTest {


    /**
     * For the basic use case of just capturing program output there is a static method:
     */

    @Test
    public void testCapturingOutput() {
        String output = ProcBuilder.run("echo", "Hello World!");

        assertEquals("Hello World!\n", output);
    }

    /**
     * There is another static method that filters a given string through
     * a program:
     */

    @Test
    public void filterStringThroughProcess() {
        String output = ProcBuilder.filter("x y z", "sed", "s/y/a/");

        assertEquals("x a z", output.trim());
    }


    /**
     *
     * Output and Input
     * ----------------
     *
     * For more control over the execution we'll use a `ProcBuilder` instance to configure
     * the process.
     *
     * The run method builds and spawns the actual process and blocks until the process exits.
     * The process takes care of writing the output to a stream, as opposed to the standard
     * facilities in the JDK that expect the client to actively consume the
     * output from an input stream:
     */
    @Test
    public void testOutputToStream() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new ProcBuilder("echo")
            .withArg("Hello World!")
            .withOutputStream(output)
            .run();

        assertEquals("Hello World!\n", output.toString());
    }

    /**
     * The input can be read from an arbitrary input stream, like this:
     */
    @Test
    public void testInputFromStream() {
        ByteArrayInputStream input = new ByteArrayInputStream("Hello cruel World".getBytes());

        ProcResult result = new ProcBuilder("wc")
            .withArgs("-w")
            .withInputStream(input).run();

        assertEquals("3", result.getOutputString().trim());
    }

    /**
     * If all you want to get is the string that gets returned and if there
     * is not a lot of data, using a streams is quite cumbersome. So for convenience
     * if no stream is provdied the output is captured by default and can be
     * obtained from the result.
     */
    @Test
    public void testUsingDefaultOutputStream() {
        ProcResult result = new ProcBuilder("echo")
            .withArg("Hello World!")
            .run();

        assertEquals("Hello World!\n", result.getOutputString());
        assertEquals(0, result.getExitValue());
        assertEquals("echo 'Hello World!'", result.getProcString());
    }

    /**
     * For providing input there is a convenience method too:
     */
    @Test
    public void testInputGetsFedIn() {
        ProcResult result = new ProcBuilder("cat")
            .withInput("This is a string").run();

        assertEquals("This is a string", result.getOutputString());
    }

    /**
     * The Environment
     * ---------------
     *
     * Some external programs are using environment variables. These can also
     * be set using the `withVar` method:
     */
    @Test
    public void testPassingInVariable() {
        ProcResult result = new ProcBuilder("bash")
            .withArgs("-c", "echo $MYVAR")
            .withVar("MYVAR", "my value").run();

        assertEquals("my value\n", result.getOutputString());
        assertEquals("bash -c 'echo $MYVAR'", result.getCommandLine());
    }

    /**
     * If you want to set multiple environment variables, you can pass a Map:
     */
    @Test
    public void testPassingInMultipleVariables() {
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("var1", "val 1");
        envVariables.put("var2", "val 2");
        ProcResult result = new ProcBuilder("bash")
                .withArgs("-c", "env")
                .withVars(envVariables).run();

        assertTrue(result.getOutputString().contains("var1=val 1\n"));
        assertTrue(result.getOutputString().contains("var2=val 2\n"));
        assertEquals("bash -c env", result.getCommandLine());
    }

    /**
     * The environment can be cleared of values inherited from the parent process:
     */
    @Test
    public void testClearEnvironment() {
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("var1", "val 1");
        envVariables.put("var2", "val 2");
        ProcResult result = new ProcBuilder("bash")
                .withArgs("-c", "env")
                .clearEnvironment()
                .withVars(envVariables).run();

        String[] outputLines = result.getOutputString().split("\n");
        assertEquals("var1=val 1", outputLines[0]);
        assertEquals("var2=val 2", outputLines[1]);
        // Note: environment is not going to be completely empty, as there are some variables that every process needs
        //       thus we only assert on the first two lines.

        assertEquals("bash -c env", result.getCommandLine());
    }


    /**
     * By default the new program is spawned in the working directory of
     * the parent process. This can be overidden:
     */
    @Test
    public void testHonorsWorkingDirectory() {
        ProcResult result = new ProcBuilder("pwd")
            .withWorkingDirectory(new File("/"))
            .run();

        assertEquals("/\n", result.getOutputString());
    }

    /**
     *
     * Timeouts
     * --------
     *
     * A common usecase for external programs is batch processing of data.
     * These programs might always run into difficulties. Therefore a timeout can be
     * specified. There is a default timeout of 5000ms. If the program does not terminate within the timeout
     * interval it will be terminated and the failure is indicated through
     * an exception:
     */
    @Test
    public void testHonorsTimeout() {
        ProcBuilder builder = new ProcBuilder("sleep")
            .withArg("2")
            .withTimeoutMillis(1000);
        try {
            builder.run();
            fail("Should time out");
        } catch (TimeoutException ex) {
            assertEquals("Process 'sleep 2' timed out after 1000ms.", ex.getMessage());
        }
    }

    /**
     * Even if the process does not timeout, we might be interested in the
     * execution time. It is also available through the result:
     */
    @Test
    public void testReportsExecutionTime() {
        ProcResult result = new ProcBuilder("sleep")
            .withArg("0.5")
            .run();

        assertTrue(result.getExecutionTime() > 500 && result.getExecutionTime() < 1000);
    }


    /**
     * In some cases you might want to disable the timeout.
     *
     * To make this explicit rather than setting the timeout to
     * a very large number there is a method to disable the
     * timeout.
     *
     * Note: Not having a timeout doesn't necessarily make your system
     * more stable. Especially if the process hangs (e.g. waiting for
     * input on stdin).
     */
    @Test
    public void testDisablingTimeout() {
        ProcBuilder builder = new ProcBuilder("sleep")
            .withArg("7")
            .withNoTimeout();

        ProcResult result = builder.run();
        assertEquals(result.getExecutionTime(), 7000, 500);
    }

    /**
     * Exit Status
     * -----------
     *
     * It is a time honoured tradition that programs signal a failure
     * by returning a non-zero exit value. However in java failure is
     * signalled through exceptions. Non-Zero exit values therefore
     * get translated into an exception, that also grants access to
     * the output on standard error.
     */
    @Test
    public void testNonZeroResultYieldsException() {
        ProcBuilder builder = new ProcBuilder("ls")
            .withArg("xyz");
        try {
            builder.run();
            fail("Should throw exception");
        } catch (ExternalProcessFailureException ex) {
            assertEquals("No such file or directory", ex.getStderr().split("\\:")[2].trim());
            assertTrue(ex.getExitValue() > 0);
            assertEquals("ls xyz", ex.getCommandLine());
            assertTrue(ex.getTime() > 0);

        }
    }

    /** [NO-DOC] */
    @Test
    public void testNonZeroResultYieldsExceptionWithOnlyStdout() {
        ProcBuilder builder = new ProcBuilder("bash")
            .withArgs("-c", "echo Standard Out; exit 1");
        try {
            builder.run();
            fail("Should throw exception");
        } catch (ExternalProcessFailureException ex) {
            assertTrue(ex.getExitValue() == 1);
            assertTrue(ex.getTime() > 0);

            final String[] messageLines = ex.getMessage().split("\n");
            assertEquals("External process `bash` terminated with unexpected exit status 1 after X:", messageLines[0].replaceAll("\\d+ms","X"));
            assertEquals("  $ bash -c 'echo Standard Out; exit 1'", messageLines[1]);
            assertEquals("  STDOUT: Standard Out", messageLines[2]);
        }
    }

    /** [NO-DOC] */
    @Test
    public void testNonZeroResultYieldsExceptionWithStdErrAndStdout() {
        ProcBuilder builder = new ProcBuilder("bash")
            .withArgs("-c", ">&2 echo Standard Error; echo Standard Out Line 1;echo Standard Out Line 2; exit 1");
        try {
            builder.run();
            fail("Should throw exception");
        } catch (ExternalProcessFailureException ex) {
            assertTrue(ex.getExitValue() == 1);
            assertTrue(ex.getTime() > 0);
            final String[] messageLines = ex.getMessage().split("\n");
            assertEquals("  STDERR: Standard Error", messageLines[2]);
            assertEquals("  STDOUT: Standard Out Line 1", messageLines[3]);
            assertEquals("  STDOUT: Standard Out Line 2", messageLines[4]);
        }
    }

    /**
     * In some cases a non-zero exit code doesn't indicate an error, but it is
     * used to return a result, e.g. with `grep`.
     *
     * In that case throwing an exception would be inappropriate. To prevent an
     * exception from being thrown we can configure the builder to ignore the exit
     * status:
     */
    @Test
    public void testHonorsIgnoreExitStatus() {
        try {
            ProcResult result = new ProcBuilder("bash")
                .withArgs("-c", "echo Hello World!;exit 100")
                .ignoreExitStatus()
                .run();

            assertEquals("Hello World!\n", result.getOutputString());
            assertEquals(100, result.getExitValue());
        } catch (ExternalProcessFailureException ex) {
            fail("A process started with ignoreExitStatus should not throw an exception");
        }
    }

    /**
     * It is also possible to specify a set of expected status codes that will not lead
     * to an exception:
     */
    @Test
    public void testHonorsDefinedExitStatuses() {
        try {
            ProcResult result = new ProcBuilder("bash")
                .withArgs("-c", "echo Hello World!;exit 100")
                .withExpectedExitStatuses(0, 100)
                .run();

            assertEquals("Hello World!\n", result.getOutputString());
            assertEquals(100, result.getExitValue());
        } catch (ExternalProcessFailureException ex) {
            fail("An expected exit status should not lead to an exception");
        }
    }

    /**
     * Status codes that are not expected will so still lead to an exception:
     */
    @Test
    public void testHonorsOnlyDefinedExitStatuses() {
        try {
            ProcResult result = new ProcBuilder("bash")
                .withArgs("-c", "echo Hello World!;exit 99")
                .withExpectedExitStatuses(0, 100)
                .run();

            fail("An exit status that is not part of the expectedExitStatuses should throw");
        } catch (ExternalProcessFailureException ex) {
            assertEquals(99, ex.getExitValue());
        }
    }

    /**
     *
     * Good to Know
     * ------------
     *
     * Input and output can also be provided as `byte[]`.
     * `ProcBuilder` also copes with large amounts of
     * data.
     */
    @Test
    public void testCopesWithLargeAmountOfData() {
        int MEGA = 1024 * 1024;
        byte[] data = new byte[4 * MEGA];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Math.round(Math.random() * 255 - 128);
        }

        ProcResult result = new ProcBuilder("gzip")
            .withInput(data)
            .run();

        assertTrue(result.getOutputBytes().length > 2 * MEGA);
    }

    /**
     * The builder allows to build and spawn several processes from
     * the same builder instance:
     */
    @Test
    public void testCanCallRunMultipleTimes() throws InterruptedException {
        ProcBuilder builder = new ProcBuilder("date");

        String date1 = builder.run().getOutputString();
        Thread.sleep(2000);
        String date2 = builder.run().getOutputString();

        assertNotNull(date1);
        assertNotNull(date2);
        assertTrue(!date1.equals(date2));
    }

    /**
     * Pipes
     * -----
     *
     * Here is how you can consume stdout in a streaming fashion (for example line by line):
     */
    @Test
    public void testLineByLinePipe() {
        new ProcBuilder("echo")
            .withArgs("line1\nline2")
            .withOutputConsumer(new StreamConsumer() {
                public void consume(InputStream stream) throws IOException {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    assertEquals("line1", reader.readLine());
                    assertEquals("line2", reader.readLine());
                    assertNull(reader.readLine());
                }
            })
            .withTimeoutMillis(2000)
            .run();
    }

    /**
     * Of course, you can consume stderr in the same way:
     */
    @Test
    public void testErrorConsumer() {
        new ProcBuilder("bash")
            .withArgs("-c", ">&2 echo error;>&2 echo error2;echo stdout")
            .withOutputConsumer(new StreamConsumer() {
                @Override
                public void consume(InputStream stream) throws IOException {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    assertEquals("stdout", reader.readLine());
                    assertNull(reader.readLine());
                }
            })
            .withErrorConsumer(new StreamConsumer() {
                @Override
                public void consume(InputStream stream) throws IOException {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    assertEquals("error", reader.readLine());
                    assertEquals("error2", reader.readLine());
                    assertNull(reader.readLine());
                }
            })
            .withTimeoutMillis(2000)
            .run();
    }

    /**
     * [NO-DOC]
     */
    @Test
    public void testLargerPipe() throws IOException, InterruptedException {
        new ProcBuilder("head")
            .withArgs("-c 10000000")
            .withArg("/dev/urandom")

            .withOutputConsumer(new StreamConsumer() {
                public void consume(InputStream stream) throws IOException {
                    int counter = 0;
                    while (stream.read() != -1) {
                        counter++;
                    }
                    assertEquals(10000000, counter);
                }
            })
            .withTimeoutMillis(20000)
            .run();
    }

    /**
     * [NO-DOC]
     */
    @Test
    public void testExceptionInConsumer() throws IOException, InterruptedException {
        try {
            new ProcBuilder("echo")
                .withArgs("line1\nline2")
                .withOutputConsumer(new StreamConsumer() {
                    public void consume(InputStream stream) throws IOException {
                        throw new IOException("Oops!");
                    }
                })
                .withTimeoutMillis(20000)
                .run();

            fail("Expected to get an Exception.");
        } catch (IllegalStateException ex){

        }
    }

    /**
     * [NO-DOC]
     */
    @Test(expected=IllegalArgumentException.class)
    public void testWithOutputConsumerClashesWithWithOutputStream() throws IOException, InterruptedException {
        new ProcBuilder("who")
            .withOutputConsumer(new StreamConsumer() {
                public void consume(InputStream stream) throws IOException {
                }
            })
            .withOutputStream(new ByteArrayOutputStream())
            .run();
    }

    /**
     * [NO-DOC]
     */
    @Test(expected=IllegalStateException.class)
    public void testGetOutputStringClashesWithWithOutputConsumer() throws IOException, InterruptedException {
        ProcResult result = new ProcBuilder("man")
            .withArgs("man")
            .withOutputConsumer(new StreamConsumer() {
                public void consume(InputStream stream) throws IOException {
                    while (stream.read() != -1) {
                    }
                }
            })
            .withTimeoutMillis(1000)
            .run();

        result.getOutputString();
    }

    /**
     * [NO-DOC]
     */
    @Test(expected=IllegalStateException.class)
    public void testGetOutputStringClashesWithWithOutputStream() throws IOException, InterruptedException {
        ProcResult result = new ProcBuilder("man")
            .withArgs("man")
            .withOutputStream(new ByteArrayOutputStream())
            .run();
        result.getOutputString();
    }

    /**
     * Error output can also be accessed directly:
     */
    @Test
    public void testErrorOutput() {
        ProcResult result = new ProcBuilder("bash")
            .withArgs("-c", ">&2 echo error;>&2 echo error2; echo out;echo out2")
            .run();

        assertEquals("out\nout2\n", result.getOutputString());
        assertEquals("error\nerror2\n", result.getErrorString());
    }

    /**
     * Alteratively an output stream can be passed in:
     */
    @Test
    public void testErrorOutputStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        new ProcBuilder("bash")
            .withArgs("-c", ">&2 echo error;>&2 echo error2; echo out;echo out2")
            .withOutputStream(out)
            .withErrorStream(err)
            .run();

        assertEquals("out\nout2\n", out.toString());
        assertEquals("error\nerror2\n", err.toString());
    }

    /**
     *
     * [NO-DOC]
     *
     * If stderr has been consumed by user provided stream, it is not available
     * for populating the {@link ExternalProcessFailureException}.
     */
    @Test
    public void testNonZeroResultYieldsExceptionAndDoesntFail() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ProcBuilder builder = new ProcBuilder("ls")
            .withArg("xyz")
            .withErrorStream(err);

        try {
            builder.run();
            fail("Should throw exception");
        } catch (ExternalProcessFailureException ex) {
            final String[] messageLines = ex.getMessage().split("\n");

            assertEquals("External process `ls` terminated with unexpected exit status X after Yms:",
                messageLines[0]
                    .replaceAll("status \\d+", "status X") // Exit status differs between gnu and bsd.
                    .replaceAll("\\d+ms", "Yms")
            );
            assertEquals("  $ ls xyz", messageLines[1]);
            assertEquals("  STDERR: <Has already been consumed.>", messageLines[2]);
            assertTrue(ex.getExitValue() > 0);
            assertEquals("ls xyz", ex.getCommandLine());
            assertTrue(ex.getTime() > 0);

        }
    }


    /**
     * String Representations
     * ----------------------
     *
     * The builder can also return a string representation of
     * the invocation. Naturally this method doesn't support chaining,
     * that means you'll have to store the builder in a variable
     * to finally run the process.
     */
    @Test
    public void testProcStringRepresentations() {

        final ProcBuilder echoBuilder = new ProcBuilder("echo")
            .withArgs("Hello World!");

        assertEquals("echo 'Hello World!'", echoBuilder.getProcString());

        ProcResult result = echoBuilder.run();
        assertEquals("Hello World!\n", result.getOutputString());
    }

    /**
     * [NO-DOC]
     * */
    @Test
    public void testStringEscaping(){
        assertEquals(
            "echo 'Hello '\"'\"'World'\"'\"'!'",
            new ProcBuilder("echo")
                .withArgs("Hello 'World'!")
                .getProcString()
        );

        assertEquals(
            "echo 'Hello\n'\"'\"'World'\"'\"'!'",
            new ProcBuilder("echo")
                .withArgs("Hello\n'World'!")
                .getProcString()
        );
    }

    /**
     * [NO-DOC]
     * */
    @Test
    public void testMutualExclusivityOfStdOutConsumption1() {
        try {
            new ProcBuilder("echo")
                .withArgs("Hello\n'World'!")
                .withOutputStream(System.out)
                .withOutputConsumer(new StreamConsumer() {
                    @Override
                    public void consume(InputStream stream) throws IOException {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        String line;
                        while ((line = reader.readLine()) != null) ;
                    }
                });
            fail("`withOutputStream` and `withOutputConsumer` are mutually exclusive");
        } catch (IllegalArgumentException iae) {
            assertEquals("`withOutputStream(OutputStream)` and `withOutputConsumer(OutputConsumer)` are mutually exclusive.", iae.getMessage());
        }
    }

    /**
     * [NO-DOC]
     * */
    @Test
    public void testMutualExclusivityOfStdOutConsumption2() {
        try {
            new ProcBuilder("echo")
                .withArgs("Hello\n'World'!")
                .withOutputConsumer(new StreamConsumer() {
                    @Override
                    public void consume(InputStream stream) throws IOException {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        String line;
                        while ((line = reader.readLine()) != null) ;
                    }
                })
                .withOutputStream(System.out);
            fail("`withOutputStream` and `withOutputConsumer` are mutually exclusive");
        } catch (IllegalArgumentException iae) {
            assertEquals("`withOutputStream(OutputStream)` and `withOutputConsumer(OutputConsumer)` are mutually exclusive.", iae.getMessage());
        }
    }


    /**
     * [NO-DOC]
     * */
    @Test
    public void testMutualExclusivityOfStdErrConsumption1() {
        try {
            new ProcBuilder("echo")
                .withArgs("Hello\n'World'!")
                .withErrorStream(System.out)
                .withErrorConsumer(new StreamConsumer() {
                    @Override
                    public void consume(InputStream stream) throws IOException {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        String line;
                        while ((line = reader.readLine()) != null) ;
                    }
                });
            fail("`withErrorStream` and `withErrorConsumer` are mutually exclusive");
        } catch (IllegalArgumentException iae) {
            assertEquals("`withErrorStream(OutputStream)` and `withErrorConsumer(OutputConsumer)` are mutually exclusive.", iae.getMessage());
        }
    }

    /**
     * [NO-DOC]
     * */
    @Test
    public void testMutualExclusivityOfStdErrConsumption2() {
        try {
            new ProcBuilder("echo")
                .withArgs("Hello\n'World'!")
                .withErrorConsumer(new StreamConsumer() {
                    @Override
                    public void consume(InputStream stream) throws IOException {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                        String line;
                        while ((line = reader.readLine()) != null) ;
                    }
                })
                .withErrorStream(System.out);
            fail("`withErrorStream` and `withErrorConsumer` are mutually exclusive");
        } catch (IllegalArgumentException iae) {
            assertEquals("`withErrorStream(OutputStream)` and `withErrorConsumer(OutputConsumer)` are mutually exclusive.", iae.getMessage());
        }
    }

    /**
     * [NO-DOC]
     * */
    @Test
    public void testProcessGetsKilledIfBlockedThreadIsInterrupted() throws InterruptedException {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProcBuilder("sleep")
                    .withArgs("7")
                    .run();
            }
        });

        thread.start();
        thread.interrupt();

        Thread.sleep(1000); // Hoping to make build more stable.
        
        // This test is a bit iffy as it relies on finding the process using ps.
        // If this test fails it will leave a dead process around for a couple of seconds.
        String[] processes = ProcBuilder.run("ps", "-ax").split("\n");
        for (String process : processes) {
            if (process.endsWith("sleep 7")) {
                fail("Process is still running after thread was interrupted:\n " + process);
            }
        }
    }
}
