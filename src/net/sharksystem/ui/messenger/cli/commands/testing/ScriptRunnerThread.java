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
        this.peerName = Helper.getFriendlyPeerName(peerName);
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
        this.ui.startCLI(peerName);
    }
}
