[![Build Status](https://travis-ci.org/fleipold/jproc.svg?branch=master)](https://travis-ci.org/fleipold/jproc)


Intro
-----

Running external commands in Java is an error prone task.
JProc helps managing input and output of external processes as
well as error conditions. It uses sensible defaults, such as throwing an
exception if a process terminates with a non zero exit code.

Five Minute Tutorial
--------------------

To get started  either download the [jar](https://oss.sonatype.org/content/repositories/releases/org/buildobjects/jproc/2/jproc-2.jar) or
if you are using maven add this snippet to your pom:

~~~ .xml
<dependency>
          <groupId>org.buildobjects</groupId>
          <artifactId>jproc</artifactId>
          <version>2.0.1</version>
</dependency>
~~~

For the basic use case of just capturing program output there is a static method:

~~~ .java
String output = ProcBuilder.run("echo", "Hello World!");

assertEquals("Hello World!\n", output);
~~~

Also there is a static method that filters a given string through
a program:

~~~ .java
String output = ProcBuilder.filter("x y z","sed" ,"s/y/a/");

assertEquals("x a z\n", output);
~~~

For more control over the execution we'll use a `ProcBuilder` instance to configure
the process.
The run method builds and spawns the actual process and blocks until the process exits.
The process takes care of writing the output to a stream, as opposed to the standard
facilities in the JDK that expect the client to actively consume the
output from an input stream:

~~~ .java
ByteArrayOutputStream output = new ByteArrayOutputStream();

new ProcBuilder("echo")
        .withArg("Hello World!")
        .withOutputStream(output)
        .run();

assertEquals("Hello World!\n", output.toString());
~~~

The input can be read from an arbitrary input stream, like this:

~~~ .java
ByteArrayInputStream input = new ByteArrayInputStream("Hello cruel World".getBytes());

ProcResult result = new ProcBuilder("wc")
        .withArgs("-w")
        .withInputStream(input).run();

assertEquals("3", result.getOutputString().trim());
~~~

If all you want to get is the string that gets returned and if there
is not a lot of data, using a streams is quite cumbersome. So for convenience
if no stream is provdied the output is captured by default and can be
obtained from the result.

~~~ .java
ProcResult result = new ProcBuilder("echo")
                            .withArg("Hello World!")
                            .run();

assertEquals("Hello World!\n", result.getOutputString());
assertEquals(0, result.getExitValue());
assertEquals("echo \"Hello World!\"", result.getProcString());
~~~

For providing input there is a convenience method too:

~~~ .java
ProcResult result = new ProcBuilder("cat")
   .withInput("This is a string").run();

assertEquals("This is a string", result.getOutputString());
~~~

Some external programs are using environment variables. These can also
be set using the `withVar` method

~~~ .java
ProcResult result = new ProcBuilder("bash")
                            .withArgs("-c", "echo $MYVAR")
                            .withVar("MYVAR","my value").run();

assertEquals("my value\n", result.getOutputString());
assertEquals("bash -c \"echo $MYVAR\"", result.getProcString());
~~~

A common usecase for external programs is batch processing of data.
These programs might always run into difficulties. Therefore a timeout can be
specified. There is a default timeout of 5000ms. If the program does not terminate within the timeout
interval it will be terminated and the failure is indicated through
an exception:

~~~ .java
ProcBuilder builder = new ProcBuilder("sleep")
        .withArg("2")
        .withTimeoutMillis(1000);
try {
    builder.run();
    fail("Should time out");
}
catch (TimeoutException ex){
    assertEquals("Process 'sleep' timed out after 1000ms.", ex.getMessage());
}
~~~

Even if the process does not timeout, we might be interested in the
execution time. It is also available through the result:

~~~ .java
ProcResult result = new ProcBuilder("sleep")
        .withArg("0.5")
        .withTimeoutMillis(1000)
        .run();

assertTrue(result.getExecutionTime() > 500 && result.getExecutionTime() < 1000);
~~~

By default the new program is spawned in the working directory of
the parent process. This can be overidden:

~~~ .java
ProcResult result = new ProcBuilder("pwd")
        .withWorkingDirectory(new File("/"))
        .run();

assertEquals("/\n", result.getOutputString());
~~~

It is a time honoured tradition that programs signal a failure
by returning a non-zero exit value. However in java failure is
signalled through exceptions. Non-zero exit values therefore
get translated into an exception, that also grants access to
the output on standard error.

~~~ .java
ProcBuilder builder = new ProcBuilder("ls")
                            .withArg("xyz");
try {
    builder.run();
    fail("Should throw exception");
} catch (ExternalProcessFailureException ex){
    assertEquals("External process 'ls' returned 1.\n" +
                 "ls: xyz: No such file or directory\n",
                 ex.getMessage());
    assertEquals("ls: xyz: No such file or directory\n", ex.getStderr());
    assertEquals(1, ex.getExitValue());
    assertEquals("ls", ex.getCommand());
}
~~~

There are times when you will call a program that normally returns
a non-zero exit value.  If the program can return one of several 
exit status values that are considered "OK", you can specify the list
of valid exit status codes.

~~~ .java
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
	assert false;
}
~~~

There are also times when it is appropriate to simply ignore the
exit status completely.

~~~ .java
try {
	ProcResult result = new ProcBuilder("bash")
							  .withArgs("-c", "echo Hello World!;exit 100")
							  .ignoreExitStatus()
							  .run();
	
	assertEquals("Hello World!\n", result.getOutputString());
    assertEquals(100, result.getExitValue());
}
catch(ExternalProcessFailureException ex) {
	assert false;
}
~~~

Input and output can also be provided as `byte[]`.
`ProcBuilder` copes with large amounts of
data:

~~~ .java
int MEGA = 1024 * 1024;
byte[] data = new byte[4 * MEGA];
for (int i = 0; i < data.length; i++) {
    data[i] = (byte) Math.round(Math.random() * 255 - 128);
}

ProcResult result = new ProcBuilder("gzip")
   .withInput(data)
   .run();

assertTrue(result.getOutputBytes().length > 2 * MEGA);
~~~

The builder allows to build and spawn several processes from
the same builder instance:

~~~ .java
ProcBuilder builder = new ProcBuilder("uuidgen");
String uuid1 = builder.run().getOutputString();
String uuid2 = builder.run().getOutputString();

assertNotNull(uuid1);
assertNotNull(uuid2);
assertTrue(!uuid1.equals(uuid2));
~~~

