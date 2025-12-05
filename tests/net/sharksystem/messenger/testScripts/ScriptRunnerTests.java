package net.sharksystem.messenger.testScripts;

import net.sharksystem.SharkException;
import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.ui.messenger.cli.ProductionUI;
import net.sharksystem.ui.messenger.cli.commands.testing.ScriptRunnerProcess;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class ScriptRunnerTests {
    @Test
    public void testTestScriptAsParameter() {
        String peerName = "Alice";
        String testName = "test0";
        String script = "sendMessages; wait 1000; lsMessages";

        try {
            String[] args = new String[3];
            args[0] = peerName + "_" + testName;
            args[1] = String.valueOf(ASAPHubManager.DEFAULT_WAIT_INTERVAL_IN_SECONDS);
            args[2] = script;
            ProductionUI ui = new ProductionUI(args);
        } catch (IOException | SharkException e) {
            System.err.println("failed: " + testName);
            e.printStackTrace();
        }
    }

    @Test
    public void testTestRunnerRuntimeExec() throws IOException {
        ScriptRunnerProcess srp =
                new ScriptRunnerProcess("Alice", "test1", "sendMessage msgInTest");

        srp.start();
    }

    @Test
    public void whatWorks() throws IOException {
        // a specified system command.
        // String command = "java -jar WriteFile.jar"; // that little prg. produced a java file - works
        String command = "java -version";

        /*
        array of strings, each element of which has environment variable settings in the format name=value,
        or null if the subprocess should inherit the environment of the current process.
         */
        String[] env = null; // works with Windows

        /*
        the working directory of the subprocess,
        or null if the subprocess should inherit the working directory of the current process.
         */
        File workingDir = null; // works with Windows

        Runtime.getRuntime().exec(command, env, workingDir);
    }
}
