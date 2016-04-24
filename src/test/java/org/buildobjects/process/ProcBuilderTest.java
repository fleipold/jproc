package org.buildobjects.process;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.junit.Assert.*;


/** [DOC]
 *  JProc Five Minute Tutorial*/
public class ProcBuilderTest {

    /**
     * To launch an external program  we'll use a <code>ProcBuilder</code>. The run method
     * builds and spawns the actual process and blocks until the process exits.
     *  The process takes care of writing the output to a stream (as opposed to the standard
     * facilities in the JDK that expect the client to actively consume the
     * output from an input stream:
     * */
    @Test
    public void testOutputToStream(){
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
    public void testInputFromStream(){
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
    public void testUsingDefaultOutputStream(){
        ProcResult result = new ProcBuilder("echo")
                                    .withArg("Hello World!")
                                    .run();

        assertEquals("Hello World!\n", result.getOutputString());
        assertEquals(0, result.getExitValue());
        assertEquals("echo \"Hello World!\"", result.getProcString());
    }

    /** For providing input there is a convenience method too: */
    @Test
    public void testInputGetsFedIn(){
        ProcResult result = new ProcBuilder("cat")
           .withInput("This is a string").run();

        assertEquals("This is a string", result.getOutputString());
    }

    /** Some external programs are using environment variables. These can also
     * be set using the <code>withVar</code> method*/
    @Test
    public void testPassingInVariable(){
        ProcResult result = new ProcBuilder("bash")
                                    .withArgs("-c", "echo $MYVAR")
                                    .withVar("MYVAR","my value").run();

        assertEquals("my value\n", result.getOutputString());
        assertEquals("bash -c \"echo $MYVAR\"", result.getProcString());
    }

    /** By default the new program is spawned in the working directory of
     * the parent process. This can be overidden: */
    @Test
    public void testHonorsWorkingDirectory(){
        ProcResult result = new ProcBuilder("pwd")
                .withWorkingDirectory(new File("/"))
                .run();

        assertEquals("/\n", result.getOutputString());
    }
    
    /** Verify that we can specify an exit code to ignore
     */
    @Test
    public void testHonorsDefinedExitStatuses() {
    	try {
    		int[] exitstatuses = {0,100};
    		ProcResult result = new ProcBuilder("bash")
    								  .withArgs("-c", "echo Hello World!;exit 100")
    								  .withExitStatuses(exitstatuses)
    								  .run();
    		
    		assertEquals("Hello World!\n", result.getOutputString());
            assertEquals(100, result.getExitValue());
    	}
    	catch(ExternalProcessFailureException ex) {
    		// We got here because the extension failed to prevent the exception from being thrown
    		assert false;
    	}
    }
    
    /** Verify that we ignore only the specified status codes
     */
    @Test
    public void testHonorsOnlyDefinedExitStatuses() {
    	try {
    		int[] exitstatuses = {0,100};
    		@SuppressWarnings("unused")
			ProcResult result = new ProcBuilder("bash")
    								  .withArgs("-c", "echo Hello World!;exit 99")
    								  .withExitStatuses(exitstatuses)
    								  .run();
    		
    		// We should never get here.
    		assert false;
    	}
    	catch(ExternalProcessFailureException ex) {
    		// We expect to get here. Consume the exception and do not throw a new one.
    		assert true;
    	}
    }
    
    /** Verify that we can ignore all non-zero exit status codes
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
    	}
    	catch(ExternalProcessFailureException ex) {
    		// We got here because the extension failed to prevent the exception from being thrown
    		assert false;
    	}
    }

    /** Even if the process does not timeout, we might be interested in the
     * execution time. It is also available through the result:*/
    @Test
    public void testReportsExecutionTime(){
        ProcResult result = new ProcBuilder("sleep")
                .withArg("0.5")
                .run();

        assertTrue(result.getExecutionTime() > 500 && result.getExecutionTime() < 1000);
    }


    /** A common usecase for external programs is batch processing of data.
     * These programs might always run into difficulties. Therefore a timeout can be
     * specified. There is a default timeout of 5000ms. If the program does not terminate within the timeout
     * interval it will be terminated and the failure is indicated through
     * an exception:*/
    @Test
    public void testHonorsTimeout(){
        ProcBuilder builder = new ProcBuilder("sleep")
                .withArg("2")
                .withTimeoutMillis(1000);
        try {
            builder.run();
            fail("Should time out");
        }
        catch (TimeoutException ex){
            assertEquals("Process 'sleep 2' timed out after 1000ms.", ex.getMessage());
        }
    }


    /** It is a time honoured tradition that programs signal a failure
     * by returning a non-zero exit value. However in java failure is
     * signalled through exceptions. Non-Zero exit values therefore
     * get translated into an exception, that also grants access to
     * the output on standard error.*/
    @Test
    public void testNonZeroResultYieldsException(){
        ProcBuilder builder = new ProcBuilder("ls")
                                    .withArg("xyz");
        try {
            builder.run();
            fail("Should throw exception");
        } catch (ExternalProcessFailureException ex){
            assertEquals("No such file or directory", ex.getStderr().split("\\:")[2].trim());
            assertTrue(ex.getExitValue() > 0);
            assertEquals("ls xyz", ex.getCommand());
            assertTrue(ex.getTime() > 0);

        }
    }



    /** The builder allows to build and spawn several processes from
     * the same builder instance: */
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

    /** For convenience there is also a static method that just runs a
     * program and captures the ouput:
     */
    @Test
    public void testStaticRun(){
        String output = ProcBuilder.run("echo", "Hello World!");

        assertEquals("Hello World!\n", output);
    }

    /** Also there is a static method that filters a given string through
     * a program:
     */
    @Test
    public void testStaticFilter(){
        String output = ProcBuilder.filter("x y z","sed" ,"s/y/a/");

        assertEquals("x a z", output.trim());
    }

    /** Input and output can also be provided as <code>byte[]</code>.
     * <code>ProcBuilder</code> also copes with large amounts of
     * data.*/
    @Test
    public void testCopesWithLargeAmountOfData(){
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


}
