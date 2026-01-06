package net.sharksystem.ui.messenger.cli.distributedtesting;

import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.app.messenger.commands.CommandNames;
import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompiler;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.List;

/**
 * class that orchestrates one distributed test scenario
 * An instance of this class runs on orchestrator side - in a Script Runner.
 * Each test is performed by an individual peer instance.
 * Code is added in the beginning of the scripts to synchronise test start.
 * Test performed - results are received by test orchestrator
 *
 */
class OrchestratedTestLauncher extends Thread {
    private static String scriptEnd_Exit;
    private final SNMAppSupportingDistributedTesting snmApp4DistributedTesting;
    final OrchestratedTest test2run;
    public static final int FIRST_ORCHESTRATOR_PORT = 1000;
    public static final String SETTLED_TAG_PREAMBLE = "peerSettled_";
    public static final String ORCHESTRATOR_PEER_NAME = "orchest";
    public static final String LAUNCH_TEST_TAG_PREAMBLE = "launchTest_";
    public final static int FINAL_WAIT_PERIODE_BEFORE_LAUNCH = 1000;
    public final static int MAX_TEST_DURATION_IN_MILLIS = 1000 * 60 * 2; // 2minutes

    public static int nextTestNumber = 0;
    private final int portNumber4ThisTest;
    private final String orchestratorScript;
    private String[] effectiveScripts;
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

        this.snmApp4DistributedTesting = snmDTesterApp;
        this.test2run = test2run;
        synchronized (OrchestratedTestLauncher.class) {
            this.testNumber = nextTestNumber++;
            // find available orchestratorPort
            this.portNumber4ThisTest = getAvailablePortNumber();
        }

        // script ends: exit;
        if (OrchestratedTestLauncher.scriptEnd_Exit == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR).
                append(CommandNames.CLI_EXIT).append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
            OrchestratedTestLauncher.scriptEnd_Exit = sb.toString();
        }

        //////////////////// produce orchestrator script //////////////////////////////////////////////////////////////
        String launchTag = LAUNCH_TEST_TAG_PREAMBLE + this.getTestID();

        // produce orchestrator script - sync and collect data
        StringBuilder sb = new StringBuilder();
        this.snmApp4DistributedTesting.tellUI(
                "note: orchestrator test runs 10 time as long as each peer (in ms): " + this.maxTestDurationInMillis);

        // set timeBomb like timeBomb 1200000;
        sb.append(CommandNames.CLI_TIME_BOMB + TestLanguageCompiler.CLI_SPACE
                + this.maxTestDurationInMillis * 10 + TestLanguageCompiler.LANGUAGE_SEPARATOR);

        // e.g. openTCP 2222;
        sb.append(CommandNames.CLI_OPEN_TCP);
        sb.append(TestLanguageCompiler.CLI_SPACE);
        sb.append(this.portNumber4ThisTest);
        sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);

        //// wait for each peer to settle
        for (int peerIndex = 0; peerIndex < this.test2run.peerScripts.size(); peerIndex++) {
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

        /// / wait for results triple of max duration.
        // wait max*3;
        sb.append(CommandNames.CLI_WAIT);
        sb.append(TestLanguageCompiler.CLI_SPACE);
        sb.append(maxTestDurationInMillis * 3);
        sb.append(TestLanguageCompiler.CLI_SEPARATOR);

        ///  force CLI to write the log files
        // lsMessages
        sb.append(CommandNames.CLI_LIST_MESSAGES);
        sb.append(TestLanguageCompiler.CLI_SEPARATOR);

        //// finish peer
        // exit;
        sb.append(scriptEnd_Exit);

        //// orchestrator script produced
        this.orchestratorScript = sb.toString();

        ////// produce test script each peer ////////////////////////////////////////////////////////////////////
        // substitute ip-address placeholder
        this.effectiveScripts = new String[this.test2run.peerScripts.size()];
        int scriptsIndex = 0;
        while(scriptsIndex < this.test2run.peerScripts.size()) {
            this.effectiveScripts[scriptsIndex] =
                    this.substituteScriptPlaceHolder(
                        this.test2run.peerScripts.get(scriptsIndex), // script
                        this.test2run.peerEnvironment // complete peer environment
                );
            scriptsIndex++;
        }

        for (int peerIndex = 0; peerIndex < this.effectiveScripts.length; peerIndex++) {
            sb = new StringBuilder();
            //// set time bomb to avoid orphan processes - value is set in test case description file
            // e.g. timeBomb 120000;
            sb.append(CommandNames.CLI_TIME_BOMB).append(TestLanguageCompiler.CLI_SPACE);
            sb.append(this.maxTestDurationInMillis).append(TestLanguageCompiler.LANGUAGE_SEPARATOR);

            // connect <orchestratorIP> <port>;
            sb.append(CommandNames.CLI_CONNECT_TCP);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            try {
                sb.append(this.snmApp4DistributedTesting.getLocalIPAddress());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append(this.portNumber4ThisTest);
            sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);

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

            //// close encounter with orchestrator
            // e.g. block launchTest_cs1_#0
            sb.append(CommandNames.CLI_CLOSE_ENCOUNTER);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append("1");
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            //// write a readable log message
            // e.g. markstep
            sb.append(CommandNames.CLI_MARKSTEP);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append("startTest_").append(getTestID());
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            // add actual script
            sb.append(this.effectiveScripts[peerIndex]);
            // e.g. markstep
            sb.append(CommandNames.CLI_MARKSTEP);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            sb.append("test_ended_").append(getTestID());
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            // add exit;
            sb.append(scriptEnd_Exit);

            // add to peer script and finish with exit in case test developer forgot
            this.effectiveScripts[peerIndex] = sb.toString();
        }
    }

    private String getTestID() {
        return this.testName + "_#" + this.testNumber;
    }

    private String getBlockTag4Peer(int peerIndex) {
        return  this.getTestID() + "_" + peerIndex;
    }

    public void run() {
        // run orchestrator script first - wait to collect logs
        Log.writeLog(this, "launching orchestrator: ");
        try {
            ScriptRunnerProcess scriptRunnerProcess =
                    new ScriptRunnerProcess(
                    ORCHESTRATOR_PEER_NAME + "_" + this.getTestID(), orchestratorScript);
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
                        this.effectiveScripts[i], // testscript to run
                        this.getTestID(),
                        peerEnvironment.peerID,
                        this.snmApp4DistributedTesting.getLocalIPAddress(),
                        this.portNumber4ThisTest,
                        this.maxTestDurationInMillis
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

    private String substituteScriptPlaceHolder(String script, List<PeerHostingEnvironmentDescription> peerEnvironments)
            throws UnknownHostException {

        int ipAddressTagStartIndex = script.indexOf(TestLanguageCompiler.IP_ADDRESS_PLACEHOLDER_START_TAG);
        while(ipAddressTagStartIndex != -1) {
            StringBuilder sb = new StringBuilder();
            sb.append(script.substring(0, ipAddressTagStartIndex));

            // get peer index
            int endTagIndex = script.indexOf(TestLanguageCompiler.PLACEHOLDER_END_TAG, ipAddressTagStartIndex+1);
            if(endTagIndex == -1)
                throw new UnknownHostException("malformed placeholder (should be %IP_n% .. n is a value) - give up");

            String peerIndexString = script.substring(
                    ipAddressTagStartIndex + TestLanguageCompiler.IP_ADDRESS_PLACEHOLDER_START_TAG.length(),
                    endTagIndex);

            int peerIndex = -1;
            try {
                peerIndex = Integer.parseInt(peerIndexString);
            }
            catch(NumberFormatException e) {
                throw new UnknownHostException("malformed placeholder (should be %IP_n% .. n is NOT a value) - give up");
            }

            // replace with actual ip-address
            try {
                PeerHostingEnvironmentDescription peerEnvironment = peerEnvironments.get(peerIndex);
                sb.append(peerEnvironment.ipAddress);
            }
            catch(RuntimeException e) {
                throw new UnknownHostException("exception when substituting ip address placeholder for peer "
                        + peerIndexString + "\n" + e.getLocalizedMessage());
            }

            // add rest of the script
            if(endTagIndex+1 < script.length()) {
                sb.append(script.substring(endTagIndex + 1));
            }

            script = sb.toString();

            // more placeholder?
            ipAddressTagStartIndex = script.indexOf(TestLanguageCompiler.IP_ADDRESS_PLACEHOLDER_START_TAG);
        }

        return script;
    }
}
