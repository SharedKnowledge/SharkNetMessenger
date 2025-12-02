package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.ui.messenger.cli.ProductionUI;

import java.io.IOException;

public class ScriptRunnerThread extends Thread {
    String peerName;
    String testName;
    String script;
    ProductionUI ui;

    public ScriptRunnerThread(String peerName, String testName, String script) {
        this.peerName = ScriptRunnerThread.getFriendlyPeerName(peerName);
        this.testName = testName;
        this.script = script;

        try {
            String[] args = new String[3];
            args[0] = this.peerName + "_" + this.testName;
            args[1] = String.valueOf(ASAPHubManager.DEFAULT_WAIT_INTERVAL_IN_SECONDS);
            args[2] = this.script;
            this.ui = new ProductionUI(args);
        } catch (IOException | SharkException e) {
            System.err.println("failed: " + testName);
            e.printStackTrace();
        }
    }

    public void run() {
        this.ui.startCLI();
    }

    public static String getFriendlyPeerName(String peerName) {
        if(peerName.length() != 1) return peerName; // already friendly - hopefully

        switch (peerName) {
            case "0":
            case "A":
                return "Alice";
            case "1":
            case "B":
                return "Bob";
            case "2":
            case "C":
                return "Clara";
            case "3":
            case "D":
                return "David";
            case "4":
            case "E":
                return "Eve";
            default: return peerName;
        }
    }
}
