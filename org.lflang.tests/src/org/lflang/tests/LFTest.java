package org.lflang.tests;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lflang.FileConfig;
import org.lflang.Target;
import org.lflang.generator.StandaloneContext;

/**
 * Information about an indexed Lingua Franca test program.
 * 
 * @author Marten Lohstroh <marten@berkeley.edu>
 *
 */
public class LFTest implements Comparable<LFTest> {
    
    /**
     * Inner class for capturing streams during a particular phase of testing,
     * such as compilation or execution.
     * 
     * @author Marten Lohstroh <marten@berkeley.edu>
     *
     */
    public static class TestPhase {

        /**
         * String buffer used to record the standard output stream.
         */
        final StringBuffer std = new StringBuffer();

        /**
         * String builder used to record the standard error stream.
         */
        final StringBuffer err = new StringBuffer();

        /**
         * Return a thread responsible for recording the standard output stream
         * of the given process.
         * A separate thread is used so that the activity can preempted.
         */
        public Thread recordStdOut(Process process) {
            return recordStream(std, process.getInputStream());
        }
        
        /**
         * Return a thread responsible for recording the error stream of the
         * given process.
         * A separate thread is used so that the activity can preempted.
         */
        public Thread recordStdErr(Process process) {
            return recordStream(err, process.getErrorStream());
        }

        /**
         * Return a thread responsible for recording the given stream.
         *
         * @param builder     The builder to append to.
         * @param inputStream The stream to read from.
         */
        private Thread recordStream(StringBuffer builder, InputStream inputStream) {
            Thread t = new Thread(() -> {
                try (Reader reader = new InputStreamReader(inputStream)) {
                    int len;
                    char[] buf = new char[1024];
                    while ((len = reader.read(buf)) > 0) {
                        builder.append(buf, 0, len);
                    }
                } catch (Exception e) {
                    builder.append("[truncated...]\n");
                }
            });
            t.start();
            return t;
        }
    }
    
    /**
     * The path to the test.
     */
    public final Path srcFile;
    
    /**
     * The name of the test.
     */
    public final String name;
    
    /**
     * The result of the test.
     * @see Result
     */
    public Result result = Result.UNKNOWN;
    
    /**
     * Object used to determine where the code generator puts files.
     */
    public FileConfig fileConfig;
    
    /**
     * 
     */
    private final Path relativePath;
    
    public ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    public ByteArrayOutputStream err = new ByteArrayOutputStream();
    
    public TestPhase exec = new TestPhase();
    
    public TestPhase compile = new TestPhase();
    
    public StringBuilder issues = new StringBuilder();
        
    public final Target target;
    
    public final Path packageRoot;
    
    public LFTest(Target target, Path srcFile, Path packageRoot) {
        this.target = target;
        this.packageRoot = packageRoot;
        
        this.srcFile = srcFile;
        this.name = packageRoot.relativize(srcFile).toString();
        this.relativePath = Paths.get(name);
    }
    
    /**
     * Implementation to allow for tests to be sorted (e.g., when added to a
     * tree set).
     */
    public int compareTo(LFTest t) {
        return this.relativePath.compareTo(t.relativePath);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof LFTest && ((LFTest) o).name.equals(this.name)) {
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
    
    public boolean hasFailed() {
        if (result == Result.TEST_PASS) {
            return false;
        }
        return true;
    }
    
    public StandaloneContext getContext() {
        return (StandaloneContext)this.fileConfig.context;
    }
    
    public String reportErrors() {
        if (this.hasFailed()) {
            StringBuilder sb = new StringBuilder(System.lineSeparator());
            sb.append("+---------------------------------------------------------------------------+").append(System.lineSeparator());
            sb.append("Failed: ").append(this.name).append(System.lineSeparator());
            sb.append("-----------------------------------------------------------------------------").append(System.lineSeparator());
            sb.append("Reason: ").append(this.result.reason).append(System.lineSeparator());
            appendIfNotEmpty("Reported issues", this.issues.toString(), sb);
            appendIfNotEmpty("Compilation error output", this.err.toString(), sb);
            appendIfNotEmpty("Compilation standard output", this.out.toString(), sb);
            appendIfNotEmpty("Execution error output", this.exec.err.toString(), sb);
            appendIfNotEmpty("Execution standard output", this.exec.std.toString(), sb);
            sb.append("+---------------------------------------------------------------------------+\n");
        return sb.toString();
        } else {
            return "";
        }
    }

    public void appendIfNotEmpty(String description, String log, StringBuilder buffer) {
        if (!log.isEmpty()) {
            buffer.append(description).append(":").append(System.lineSeparator());
            buffer.append(log).append(System.lineSeparator());
        }
    }
    
    public enum Result {
        UNKNOWN("No information available."),
        CONFIG_FAIL("Could not apply configuration."),
        PARSE_FAIL("Unable to parse test."),
        VALIDATE_FAIL("Unable to validate test."),
        CODE_GEN_FAIL("Error while generating code for test."),
        BUILD_FAIL("Error while building test."),
        NO_EXEC_FAIL("Did not execute test."),
        TEST_FAIL("Test did not pass."),
        TEST_TIMEOUT("Test timed out."),
        TEST_PASS("Test passed.");

        public final String reason;
        
        private Result(String message) {
            this.reason = message;
        }
    }
}
