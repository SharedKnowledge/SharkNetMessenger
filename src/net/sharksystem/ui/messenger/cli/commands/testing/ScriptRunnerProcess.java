package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptRunnerProcess {
    private final ProcessBuilder pb;

    public ScriptRunnerProcess(String peerName, String testName, String script) throws IOException {
        // produce command line to launch a new SNM CLI
        peerName = Helper.getFriendlyPeerName(peerName);
        peerName = peerName + "_" + testName;

        Log.writeLog(this, "building process: " + peerName + " | " + script);

        List<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(Helper.SNM_CLI_JAR_FILENAME);
        args.add(peerName);
        args.add(String.valueOf(ASAPHubManager.DEFAULT_WAIT_INTERVAL_IN_SECONDS));
        args.add(script);
        this.pb = new ProcessBuilder(args);

        File log = new File(peerName + "_uiOutErr.txt");
        this.pb.redirectErrorStream(true);
        this.pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
    }

    public void start() throws IOException {
        this.pb.start();
    }
}