package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.ui.messenger.cli.ProductionUI;
import net.sharksystem.utils.Log;

import java.io.File;
import java.io.IOException;

public class ScriptRunnerProcess {
    private String command;

    public ScriptRunnerProcess(String peerName, String testName, String script) throws IOException {
        // produce command line to launch a new SNM CLI
        peerName = Helper.getFriendlyPeerName(peerName);

        StringBuilder sb = new StringBuilder();
        sb.append("java -jar ");
        sb.append(Helper.SNM_CLI_JAR_FILENAME);
        sb.append(" ");
        sb.append(peerName + "_" + testName);
        sb.append(" ");
        sb.append(String.valueOf(ASAPHubManager.DEFAULT_WAIT_INTERVAL_IN_SECONDS));
        sb.append(" \"");
        sb.append(script);
        sb.append("\"");

        // a specified system command.
        this.command = sb.toString();
    }
    public void start() throws IOException {
        /*
        array of strings, each element of which has environment variable settings in the format name=value,
        or null if the subprocess should inherit the environment of the current process.
         */
        String[] env = null;

        /*
        the working directory of the subprocess,
        or null if the subprocess should inherit the working directory of the current process.
         */
        File workingDir = null;

        Log.writeLog(this, "launch new process: " + this.command);
        System.out.println("launch new process: " + this.command);
        Runtime.getRuntime().exec(this.command, env, workingDir);
    }
}