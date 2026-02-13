package net.sharksystem.ui.messenger.cli.distributedtesting;

import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptRunnerProcess {
    private final ProcessBuilder pb;
    public static final String LOG_EXTENSION = "_uiOutErr.txt";

    public ScriptRunnerProcess(String peerName, String script) throws IOException {
        // produce command line to launch a new SNM CLI
        peerName = getFriendlyPeerName(peerName);

        Log.writeLog(this, "building process: " + peerName + " | " + script);

        List<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(SNM_CLI_JAR_FILENAME);
        args.add(peerName);
        args.add(String.valueOf(ASAPHubManager.DEFAULT_WAIT_INTERVAL_IN_SECONDS));
        args.add(script);

        System.out.println(">>>>>>>>>> START PROCESS: >>>>>>>>>>>>>>>>\n"
                + args + "\n <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        this.pb = new ProcessBuilder(args);

        File log = new File(peerName + LOG_EXTENSION);
        this.pb.redirectErrorStream(true);
        this.pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
    }

    public void start() throws IOException {
        this.pb.start();
    }

    public static final String SNM_CLI_JAR_FILENAME = "SharkNetMessengerCLI.jar";

    static String getFriendlyPeerName(String peerName) {
        if(peerName.length() != 1) return peerName; // already friendly - hopefully

        switch (peerName) {
            case "0":
            case "A":
                return "P1";
            case "1":
            case "B":
                return "P2";
            case "2":
            case "C":
                return "P3";
            case "3":
            case "D":
                return "P4";
            case "4":
            case "E":
                return "P5";
            default: return peerName;
        }
    }

}