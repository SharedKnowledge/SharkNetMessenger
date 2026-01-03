package net.sharksystem.ui.messenger.cli.distributedtesting;

import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.app.messenger.commands.CommandNames;
import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompiler;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;

/**
 * class that orchestrates one distributed test scenario
 * An instance of this class runs on orchestrator side - in a Script Runner.
 * Each test is performed by an individual peer instance.
 * Code is added in the beginning of the scripts to synchronise test start.
 * Test performed - results are received by test orchestrator
 *
 */
class OrchestratedTestLauncher extends Thread {
    private final SNMAppSupportingDistributedTesting snmApp4DistributedTesting;
    final OrchestratedTest test2run;
    public static final int FIRST_ORCHESTRATOR_PORT = 1000;
    public static final String SETTLED_TAG_PREAMBLE = "peerSettled_";
    public static final String ORCHESTRATOR_PEER_NAME = "orchest";
    public static final String LAUNCH_TEST_TAG_PREAMBLE = "launchTest_";
    public final static int FINAL_WAIT_PERIODE_BEFORE_LAUNCH = 1000;
    public final static int MAX_TEST_DURATION_IN_MILLIS = 1000 * 60 * 2; // 2minutes

    private static String scriptStartOrchestrator_SyncWithPeers = null;
    private static String scriptStartPeer_SyncWithOrchestator = null;
    private static String scriptEnd_Exit = null;
    private static String scriptSetTimeBomb;

    public static int nextTestNumber = 0;
    public int testNumber = 0;

    private int maxTestDurationInMillis = MAX_TEST_DURATION_IN_MILLIS;
    private String testName;

    // sync with other orchestrated tests
    private static synchronized int getAvailablePortNumber() {
        int port = FIRST_ORCHESTRATOR_PORT;

        ServerSocket srvSocket = null;
        while (srvSocket == null && port < 65535) {
            try {
                srvSocket = new ServerSocket(port);
                // got a port - close port - will be opened again in a few millis
                srvSocket.close();
                return port;
            } catch (IOException e) {
                // taken
            }
            port++;
        }

        if (srvSocket == null) {
            System.err.println("your system uses any thinkable orchestratorPort - amazing - give up");
            Log.writeLogErr(OrchestratedTestLauncher.class,
                    "not a single orchestratorPort available on this system - hard to imagine - give up");
            System.exit(1);
        }
        // never reach this point
        return -1;
    }

    OrchestratedTestLauncher(SNMAppSupportingDistributedTesting snmDTesterApp,
                             OrchestratedTest test2run)
            throws UnknownHostException {

        this.maxTestDurationInMillis = test2run.maxDurationInMilli;
        this.testName = test2run.testName;

        this.scriptSetTimeBomb =
                CommandNames.CLI_TIME_BOMB + TestLanguageCompiler.CLI_SPACE
                        + this.maxTestDurationInMillis + TestLanguageCompiler.LANGUAGE_SEPARATOR;

        this.snmApp4DistributedTesting = snmDTesterApp;
        this.test2run = test2run;
        synchronized (OrchestratedTestLauncher.class) {
            this.testNumber = nextTestNumber++;
        }

        // find available orchestratorPort
        int portNumber4ThisTest = getAvailablePortNumber();

        ///// init scripts used for each test run

        // orchestrator opens port, e.g. openTCP 2222;
        if (OrchestratedTestLauncher.scriptStartOrchestrator_SyncWithPeers == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(CommandNames.CLI_OPEN_TCP);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append(portNumber4ThisTest);
            sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
            OrchestratedTestLauncher.scriptStartOrchestrator_SyncWithPeers = sb.toString();
        }

        // peer connects to orchestrator, e.g. connectTCP <orchestrator-IPAddress> 2222;
        if (OrchestratedTestLauncher.scriptStartPeer_SyncWithOrchestator == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(CommandNames.CLI_CONNECT_TCP);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append(snmDTesterApp.getLocalIPAddress());
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append(portNumber4ThisTest);
            sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
            OrchestratedTestLauncher.scriptStartPeer_SyncWithOrchestator = sb.toString();
        }

        // script ends: exit;
        if (OrchestratedTestLauncher.scriptEnd_Exit == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
            sb.append(CommandNames.CLI_EXIT);
            sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
            OrchestratedTestLauncher.scriptEnd_Exit = sb.toString();
        }
    }

    private String getTestID() {
        return this.testName + "_#" + this.testNumber;
    }

    private String getBlockTag4Peer(int peerIndex) {
        return  this.getTestID() + "_" + peerIndex;
    }

    public void run() {
        String launchTag = LAUNCH_TEST_TAG_PREAMBLE + this.getTestID();

        // produce orchestrator script - sync and collect data
        StringBuilder sb = new StringBuilder();
        this.snmApp4DistributedTesting.tellUI(
                "note: orchestrator test runs 10 time as long as each peer (in ms): " + this.maxTestDurationInMillis);

        // set timeBomb like timeBomb 1200000;
        sb.append(CommandNames.CLI_TIME_BOMB + TestLanguageCompiler.CLI_SPACE
                + this.maxTestDurationInMillis * 10 + TestLanguageCompiler.LANGUAGE_SEPARATOR);

        // e.g. openTCP 2222;
        sb.append(OrchestratedTestLauncher.scriptStartOrchestrator_SyncWithPeers);
        //// wait for each peer to settle
        for (int peerIndex = 0; peerIndex < this.test2run.scripts.size(); peerIndex++) {
            // block peerSettled_cs1_#0_1; ... for up to n peers... block peerSettled_cs1_#0_n;
            sb.append(CommandNames.CLI_BLOCK);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            // wait for peer until settled
            sb.append(SETTLED_TAG_PREAMBLE);
            sb.append(this.getBlockTag4Peer(peerIndex));
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);
        }

        //// after peers told to be ready - tell them to start test
        // release launchTest_cs1_#0
        sb.append(CommandNames.CLI_RELEASE);
        sb.append(TestLanguageCompiler.CLI_SPACE);
        // release tag - tell peers to start - handshake way 2
        sb.append(launchTag);
        sb.append(TestLanguageCompiler.CLI_SEPARATOR);

        // TODO collect results - will be a single lsMessages in the right channel.

        // finish peer
        sb.append(scriptEnd_Exit);

        //// orchestrator script produced
        String orchestratorScript = sb.toString();

        //// produce script for each peer
        String[] effectiveScripts = new String[this.test2run.scripts.size()];
        for (int peerIndex = 0; peerIndex < this.test2run.scripts.size(); peerIndex++) {
            sb = new StringBuilder();
            //// set time bomb to avoid orphan processes - value is set in test case description file
            // e.g. timeBomb 120000;
            sb.append(scriptSetTimeBomb);
            // add open connection to orchestrator
            sb.append(scriptStartPeer_SyncWithOrchestator);

            //// tell orchestrator settled
            // e.g. release peerSettled_cs1_#0_1;
            sb.append(CommandNames.CLI_RELEASE);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            // handshake way 1 - tell orchestrator settled
            sb.append(SETTLED_TAG_PREAMBLE);
            sb.append(this.getBlockTag4Peer(peerIndex));
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            //// wait until orchestrator tells to launch test - handshake 2
            // e.g. block launchTest_cs1_#0
            sb.append(CommandNames.CLI_BLOCK);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append(launchTag);
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            //// and better wait a moment - won't hurt
            // wait 1000;
            sb.append(CommandNames.CLI_WAIT);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append(FINAL_WAIT_PERIODE_BEFORE_LAUNCH);
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            // TODO send results - will be a couple of sendMessages with log files in the right channel.

            // add to peer script and finish with exit in case test developer forgot
            effectiveScripts[peerIndex] = sb.toString() + this.test2run.scripts.get(peerIndex) + scriptEnd_Exit;
        }

        // run orchestrator script first - wait to collect logs
        Log.writeLog(this, "launching orchestrator: ");
        try {
            ScriptRunnerProcess scriptRunnerProcess =
                    new ScriptRunnerProcess(ORCHESTRATOR_PEER_NAME,
                            this.getTestID(), orchestratorScript);
            scriptRunnerProcess.start();
        } catch (IOException e) {
            String log = "could not start orchestrator process / don't send scripts to peers: " + e.getStackTrace();
            snmApp4DistributedTesting.tellUIError(log);
            Log.writeLogErr(this, log);
            return;
        }

        /* // test runner as thread - decided to run it in a process.
        ScriptRunnerThread scriptRunnerThread =
                new ScriptRunnerThread(ORCHESTRATOR_PEER_NAME,
                        Integer.toString(this.testNumber), orchestratorScript);
        SharkNetMessengerAppSupportingDistributedTesting.this.tellUI(
                "running script as peer " + ORCHESTRATOR_PEER_NAME + ": " + orchestratorScript);
        scriptRunnerThread.start();
         */

        //// now - send script to each peer

        // to avoid even the slightest chance of a race condition - make a little break;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        this.snmApp4DistributedTesting.tellUI("sending test scripts to peers");
        Log.writeLog(this, "sending test scripts to peers");

        try {
            for (int i = 0; i < this.test2run.peerEnvironment.size(); i++) {
                PeerHostingEnvironmentDescription peerEnvironment = this.test2run.peerEnvironment.get(i);
                TestScriptDescription testScriptDescription = new TestScriptDescription(
                        peerEnvironment.toString(), // peer IP Address
                        i, // peerName
                        effectiveScripts[i], // testscript to run
                        this.getTestID(),
                        peerEnvironment.peerID
                );

                // send message
                this.snmApp4DistributedTesting.getSharkMessengerComponent().sendSharkMessage(
                        SNMAppSupportingDistributedTesting.TEST_SCRIPT_FORMAT,
                        testScriptDescription.getMessageBytes(),
                        SNMAppSupportingDistributedTesting.TEST_SCRIPT_CHANNEL, // specific channel
                        peerEnvironment.ipAddress, // ip address as peer address
                        false, // no signing
                        false // no encryption
                );
                Log.writeLog(this, "sent script: ip | test# | script"
                        + peerEnvironment.peerID + " | "
                        + this.testNumber + " | "
                        + effectiveScripts[i]);

                this.snmApp4DistributedTesting.
                        tellUI("test scripts sent to " + peerEnvironment.peerID + "@" + peerEnvironment.ipAddress);
            }
        } catch (IOException | SharkNetMessengerException ioe) {
            this.snmApp4DistributedTesting.
                    tellUIError("cannot send test scripts / abort test# " + this.testNumber +
                            " / " + ioe.getLocalizedMessage());
        }

        ///  test launcher launched a test - this thread ends here - orchestrator process is running.
    }
}
