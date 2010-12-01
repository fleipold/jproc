#!/bin/bash

fail_build ()
{
	echo BUILD FAILED
	exit 1
}

rm -rf target
mkdir target

mkdir -p target/classes
javac -target 1.5 -d target/classes  `find src/ -name *.java`
if [ $? -ne 0 ]; then
	fail_build
fi

mkdir -p target/test-classes
javac -target 1.5 -d target/test-classes -cp target/classes:lib/junit-4.5.jar -encoding utf8 `find test -name *.java`
if [ $? -ne 0 ]; then
	fail_build
fi

java -cp target/test-classes:target/classes:lib/junit-4.5.jar org.junit.runner.JUnitCore org.buildobjects.process.ProcBuilderTest
if [ $? -ne 0 ]; then
	fail_build
fi

jar cf target/jproc.jar -C target/classes .
if [ $? -ne 0 ]; then
	fail_build
fi

jar cf target/jproc-src.jar -C src .
if [ $? -ne 0 ]; then
	fail_build
fi

mkdir -p target/javadoc
javadoc -public -d target/javadoc -sourcepath src/ org.buildobjects.process
if [ $? -ne 0 ]; then
	fail_build
fi

jar cf target/jproc-javadoc.jar -C target/javadoc .
if [ $? -ne 0 ]; then
	fail_build
fi


echo BUILD SUCCESSFUL
exit 0