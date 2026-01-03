package net.sharksystem.ui.messenger.cli;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.app.messenger.commands.CommandNames;
import net.sharksystem.app.messenger.commands.testing.OrchestrationScriptConstants;
import net.sharksystem.app.messenger.commands.testing.PeerHostingEnvironmentDescription;
import net.sharksystem.app.messenger.commands.testing.ScriptRunnerProcess;
import net.sharksystem.app.messenger.commands.testing.TestScriptDescription;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.ui.messenger.cli.testlanguage.TestLanguageCompiler;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.*;

public class SharkNetMessengerAppSupportingDistributedTesting extends SharkNetMessengerApp {
    public static final CharSequence TEST_BLOCK_RELEASE_CHANNEL = "snm://block_release";
    public static final CharSequence SCRIPT_RQ_CHANNEL = "snm://scriptRQ";
    public static final String PEER_HOST_DESCRIPTION_FORMAT = "snm/peerHostDesc";
    public static final String TEST_SCRIPT_FORMAT = "snm/testScript";
    public static final CharSequence TEST_SCRIPT_CHANNEL = "snm://testScripts";

    private PeerHostingEnvironmentDescription myEnvironment;

    private String getLocalIPAddress() throws UnknownHostException {
        if(this.myEnvironment == null) this.myEnvironment = new PeerHostingEnvironmentDescription(this.getPeerName());
        return this.myEnvironment.ipAddress;
    }

    public SharkNetMessengerAppSupportingDistributedTesting(String peerName, PrintStream out, PrintStream err)
            throws SharkException, IOException {
        this(peerName, 60*10, out, err);
    }

    public SharkNetMessengerAppSupportingDistributedTesting(String peerName, int syncWithOthersInSeconds, PrintStream out, PrintStream err)
            throws SharkException, IOException {
        super(peerName, syncWithOthersInSeconds, out, err);

        // add listener that notifies about test related messages
        this.getSharkMessengerComponent().addSharkMessagesReceivedListener(
                new SNMDistributedTestsMessageReceivedListener(this));
    }

    public String produceStringForMessage(CharSequence contentType, byte[] content) {
        if(contentType.toString().equalsIgnoreCase(PEER_HOST_DESCRIPTION_FORMAT)) {
            try {
                return new PeerHostingEnvironmentDescription(content).toString();
            } catch (IOException | ASAPException e) {
                return "known format - malformed content";
            }
        }
        else if(contentType.toString().equalsIgnoreCase(TEST_SCRIPT_FORMAT)) {
            try {
                return new TestScriptDescription(content).toString();
            } catch (IOException | ASAPException e) {
                return "known format - malformed content";
            }
        }
        return super.produceStringForMessage(contentType, content);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                     block / release                                     //
    /////////////////////////////////////////////////////////////////////////////////////////////
    private Set<String> receivedLabels = new HashSet<>(); // more instances of the same label has no specific semantics
    private List<Thread> blockedThreads = new ArrayList<>(); // a thread can wait for more labels - useful or not
    public void block(String label) {
        // check if already released
        this.tellUI("looking for release label " + label);
        while(true) {
            for(String receivedLabel : this.receivedLabels) {
                if (receivedLabel.equalsIgnoreCase(label)) {
                    this.tellUI("release label received: " + label);
                    return; // block released
                }
            }
            try {
                this.blockedThreads.add(Thread.currentThread());
                this.tellUI("threads sleeps and waits for release label " + label);
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // new message received - try again
            }
            this.tellUI("threads woke up - check if release label arrived " + label);
            this.blockedThreads.remove(Thread.currentThread());
        }
    }

    void releaseReceived(CharSequence releaseChannelURI) {
        Set<String> newReceivedLabels = new HashSet<>();
        try {
            SharkNetMessengerChannel releaseChannel =
                    this.getSharkMessengerComponent().getChannel(releaseChannelURI);

            SharkNetMessageList messages = releaseChannel.getMessages();
            for(int i = 0; i < messages.size(); i++) {
                SharkNetMessage sharkMessage = messages.getSharkMessage(i, true);
                byte[] content = sharkMessage.getContent();
                CharSequence releaseLabel = SerializationHelper.bytes2characterSequence(content);
                newReceivedLabels.add(releaseLabel.toString());
            }
            // replace old with new list
            this.receivedLabels = newReceivedLabels;

            // let's check waiting threads again
            for(Thread blockedThread : this.blockedThreads) {
                blockedThread.interrupt();
            }
        } catch (SharkNetMessengerException | IOException | ASAPException e) {
            this.tellUIError("problems reading message in channel " + releaseChannelURI);
            this.tellUIError(e.getLocalizedMessage());
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                        test run                                         //
    /////////////////////////////////////////////////////////////////////////////////////////////

    private boolean beTestPeer = false;
    public void becomeTestPeer() throws IOException, SharkException {
        this.beTestPeer = true;
        // collect information
        CharSequence peerID = this.getSharkPeer().getPeerID();
        PeerHostingEnvironmentDescription scriptRQMessage =
                new PeerHostingEnvironmentDescription(peerID.toString());

        // send message
        this.getSharkMessengerComponent().sendSharkMessage(
                SharkNetMessengerAppSupportingDistributedTesting.PEER_HOST_DESCRIPTION_FORMAT,
                scriptRQMessage.getMessageBytes(),
                SharkNetMessengerAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL, // specific channel
                (CharSequence) null, // no specific receiver
                false, // no signing
                false // no encryption
        );
        this.tellUI("script request sent: " + scriptRQMessage);
        Log.writeLog(this, "script request sent: " + scriptRQMessage);
    }

    private int lastReceivedScriptIndex = -1;
    private List<TestScriptDescription> handledScripts = new ArrayList<>();
    // peers willing to execute test script sent its environment description
    public void receivedTestScript(CharSequence testScriptChannel) {
        // less likely but more than one script could be received
        synchronized (SharkNetMessengerAppSupportingDistributedTesting.this) {
            if (!this.beTestPeer) {
                this.tellUI("test script received but no tester peer - ignored");
                return;
            }
            // add information of this new volunteer and see if we have enough participant for one new test
            try {
                SharkNetMessageList scriptMessages =
                        this.getSharkMessengerComponent().getChannel(testScriptChannel).getMessages();
                for (int scriptIndex = this.lastReceivedScriptIndex + 1; scriptIndex < scriptMessages.size(); scriptIndex++) {
                    this.lastReceivedScriptIndex = scriptIndex; // update each round - before possible exceptions

                    SharkNetMessage testSharkMessage = scriptMessages.getSharkMessage(scriptIndex, false);
                    TestScriptDescription testScriptDescription =
                            new TestScriptDescription(testSharkMessage.getContent());

                    // for me?
                    if (testScriptDescription.peerID.equalsIgnoreCase(this.getSharkPeer().getPeerID().toString())) {
                        this.tellUI("test script received - prepare script runner" + testScriptDescription);
                        Log.writeLog(this, "test script addressed to me received: " + testScriptDescription);

                        // this is script is for - alright. Is it a copy?
                        boolean copy = false;
                        for (TestScriptDescription oldScript : this.handledScripts) {
                            Log.writeLog(this, "copy? " + testScriptDescription + " | " + oldScript);
                            if (TestScriptDescription.same(oldScript, testScriptDescription)) {
                                copy = true;
                                break;
                            }
                        }
                        if (copy) {
                            String log = "received copy of an already handled test case, ignore: " + testScriptDescription;
                            Log.writeLog(this, log);
                            this.tellUI(log);
                            continue;
                        }

                        /*
                        // produce test running thread
                        new ScriptRunnerThread(
                            Integer.toString(testScriptDescription.peerIndex),
                            Integer.toString(testScriptDescription.testNumber),
                            testScriptDescription.script).start();
                         */
                        // produce test running process
                        new ScriptRunnerProcess(
                                Integer.toString(testScriptDescription.peerIndex),
                                Integer.toString(testScriptDescription.testNumber),
                                testScriptDescription.script).start();
                        this.tellUI("running script: " + testScriptDescription.script);
                        Log.writeLog(this, "script running, remember to avoid redo it: " + testScriptDescription);
                        this.handledScripts.add(testScriptDescription);
                    }
                    /*
                    else {
                        this.tellUI("test script received, not for me though.");
                        Log.writeLog(this, "test script received, not for me: " + testScriptDescription);
                    }
                     */
                }
            } catch (IOException | SharkException e) {
                String log = "problems handling test script received channel: " + e.getLocalizedMessage();
                this.tellUIError(log);
                Log.writeLogErr(this, log);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                     test orchestration                                  //
    /////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, PeerHostingEnvironmentDescription> availablePeers = new HashMap<>();

    private class OrchestratedTest {
        List<PeerHostingEnvironmentDescription> peerEnvironment;
        List<String> scripts;
        int maxDurationInMilli;
        String testName;

        OrchestratedTest(
            List<PeerHostingEnvironmentDescription> requiredPeerEnvironment,
            List<String> scripts,
            int maxDurationInMilli,
            String testName) {
            this.peerEnvironment = requiredPeerEnvironment;
            this.scripts = scripts;
            this.maxDurationInMilli = maxDurationInMilli;
            this.testName = testName;
        }
    }

    private boolean beTestOrchestrator = false;
    private List<OrchestratedTest> orchestratedTestsWaiting = new ArrayList<>();
    private List<OrchestratedTest> orchestratedTestsReady = new ArrayList<>();

    public void orchestrateTest(
            List<PeerHostingEnvironmentDescription> requiredPeerEnvironment,
            List<String> scripts, int maxDurationInMillis, String testname) {
        this.beTestOrchestrator = true;
        if(scripts == null || scripts.isEmpty()) {
            this.tellUIError("scripts must not be empty");
            return;
        }
        if(requiredPeerEnvironment == null) requiredPeerEnvironment = new ArrayList<>();
        if(requiredPeerEnvironment.size() > scripts.size()) {
            this.tellUIError("more required peers than scripts... Makes no sense. Give up");
            return;
        }
        if(requiredPeerEnvironment.size() < scripts.size()) {
            this.tellUI("less peer requirements than scripts... will take any available peer for remaining scripts");
            while(requiredPeerEnvironment.size() < scripts.size()) {
                requiredPeerEnvironment.add(
                        new PeerHostingEnvironmentDescription(null,null,null,null));
            }
        }

        this.orchestratedTestsWaiting.add(
                new OrchestratedTest(requiredPeerEnvironment, scripts, maxDurationInMillis, testname));
    }

    private int lastScriptRQIndex = -1;
    // peers willing to execute test script sent its environment description
    public void receivedScriptRQ(CharSequence scriptRQChannel) {
        synchronized (SharkNetMessengerAppSupportingDistributedTesting.this) {
            if (!this.beTestOrchestrator) {
                this.tellUI("script request received - ignore - not a test orchestrator");
                return;
            } else {
                Log.writeLog(this, "script request received - try to stage a test");
                this.tellUI("script request received - try to stage a test");
            }
            // add information of this new volunteer and see if we have enough participant for one new test
            synchronized (this) {
                try {
                    SharkNetMessageList rqMessages =
                            this.getSharkMessengerComponent().getChannel(scriptRQChannel).getMessages();
                    for (int rqIndex = this.lastScriptRQIndex + 1; rqIndex < rqMessages.size(); rqIndex++) {
                        this.lastScriptRQIndex = rqIndex; // update each round - before possible exceptions

                        SharkNetMessage rqSharkMessage = rqMessages.getSharkMessage(rqIndex, false);
                        PeerHostingEnvironmentDescription peerHostDescription =
                                new PeerHostingEnvironmentDescription(rqSharkMessage.getContent());

                        // add or replace information
                        this.availablePeers.put(peerHostDescription.peerID, peerHostDescription);
                        String log = "added volunteering peer: " + peerHostDescription;
                        this.tellUI(log);
                        Log.writeLog(this, log);

                        // try to set up a test(s).
                        this.stageTests();
                    }
                } catch (SharkNetMessengerException | IOException | ASAPException e) {
                    this.tellUIError("problems handling script RQ channel: " + e.getLocalizedMessage());
                    Log.writeLogErr(this, "problems handling script RQ channel: " + e.getLocalizedMessage());
                }
            }
        }
    }

    /** find participant of new test runs */
    private void stageTests() throws UnknownHostException {
        TestEnsemble testEnsemble = null;
        do {
            testEnsemble = this.findFittingPeers();
            if (testEnsemble != null) {
                String log = "found enough peers for a new test run: test#" + testEnsemble.waitingTestIndex;
                this.tellUI(log);
                Log.writeLog(this, log);
                // we found an ensemble to run a test
                OrchestratedTest waitingTest = this.orchestratedTestsWaiting.get(testEnsemble.waitingTestIndex);
                // add actual test ensemble
                OrchestratedTest readyTest = new OrchestratedTest(
                        testEnsemble.peerEnvironment, // replace requirements with actual available matching peer
                        waitingTest.scripts, // copy scripts
                        waitingTest.maxDurationInMilli,
                        waitingTest.testName
                );
                // new read test created
                this.orchestratedTestsReady.add(readyTest);

                // we need an int - Integer would not work since it is interpreted as key object rather index value
                int index2Remove = testEnsemble.waitingTestIndex;
                // remove from waiting list
                this.orchestratedTestsWaiting.remove(index2Remove);

                // make peers unavailable
                for(PeerHostingEnvironmentDescription env : testEnsemble.peerEnvironment) {
                    String key = env.peerID;
                    this.availablePeers.remove(key);
                }
            }
        } while(testEnsemble != null); // do again until there is not further match

        if(!this.orchestratedTestsReady.isEmpty()) {
            // launch tests
            this.tellUI("test staged .. going to launch");
            Log.writeLog(this, "test(s) staged .. going to launch");
            this.launchTests();
        } else {
            this.tellUI("couldn't stage a test - not enough peers");
            Log.writeLog(this, "couldn't stage a test - not enough peers");
        }
    }

    /** class that orchestrates one distributed test scenario
     * An instance of this class runs on orchestrator side - in a Script Runner.
     * Each test is performed by an individual peer instance.
     * Code is added in the beginning of the scripts to synchronise test start.
     * Test performed - results are received by test orchestrator
     * */
    private class OrchestratedTestLauncher extends Thread {
        final OrchestratedTest test2run;
        public static final int FIRST_ORCHESTRATOR_PORT = 1000;
        public static final String SETTLED_TAG_PREAMBLE = "peerSettled_";
        public static final String ORCHESTRATOR_PEER_NAME = "orchest";
        public static final String LAUNCH_TEST_TAG_PREAMBLE = "launchTest_";
        public final static int FINAL_WAIT_PERIODE_BEFORE_LAUNCH = 1000;
        public final static int MAX_TEST_DURATION_IN_MILLIS = 1000*60*2; // 2minutes

        private static String scriptStartOrchestrator_SyncWithPeers = null;
        private static String scriptStartPeer_SyncWithOrchestator = null;
        private static String scriptEnd_Exit = null;
        private static String scriptSetTimeBomb =
                CommandNames.CLI_TIME_BOMB + TestLanguageCompiler.CLI_SPACE
                        + MAX_TEST_DURATION_IN_MILLIS + TestLanguageCompiler.LANGUAGE_SEPARATOR;

        public static int nextTestNumber = 0;
        public int testNumber = 0;

        // sync with other orchestrated tests
        private static synchronized int getAvailablePortNumber() {
            int port = FIRST_ORCHESTRATOR_PORT;

            ServerSocket srvSocket = null;
            while(srvSocket == null && port < 65535) {
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

            if(srvSocket == null) {
                System.err.println("your system uses any thinkable orchestratorPort - amazing - give up");
                Log.writeLogErr(SharkNetMessengerAppSupportingDistributedTesting.OrchestratedTestLauncher.class,
                        "not a single orchestratorPort available on this system - hard to imagine - give up");
                System.exit(1);
            }
            // never reach this point
            return -1;
        }

        OrchestratedTestLauncher(OrchestratedTest test2run) throws UnknownHostException {
            this.test2run = test2run;
            synchronized (OrchestratedTestLauncher.class) {
                this.testNumber = nextTestNumber++;
            }

            // find available orchestratorPort
            int portNumber4ThisTest = getAvailablePortNumber();

            // init class member
            if(OrchestratedTestLauncher.scriptStartOrchestrator_SyncWithPeers == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(CommandNames.CLI_OPEN_TCP);
                sb.append(TestLanguageCompiler.CLI_SPACE);
                sb.append(portNumber4ThisTest);
                sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
                OrchestratedTestLauncher.scriptStartOrchestrator_SyncWithPeers = sb.toString();
            }

            if(OrchestratedTestLauncher.scriptStartPeer_SyncWithOrchestator == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(CommandNames.CLI_CONNECT_TCP);
                sb.append(TestLanguageCompiler.CLI_SPACE);
                sb.append(SharkNetMessengerAppSupportingDistributedTesting.this.getLocalIPAddress());
                sb.append(TestLanguageCompiler.CLI_SPACE);
                sb.append(portNumber4ThisTest);
                sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
                OrchestratedTestLauncher.scriptStartPeer_SyncWithOrchestator = sb.toString();
            }

            if(OrchestratedTestLauncher.scriptEnd_Exit == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
                sb.append(CommandNames.CLI_EXIT);
                sb.append(TestLanguageCompiler.LANGUAGE_SEPARATOR);
                OrchestratedTestLauncher.scriptEnd_Exit = sb.toString();
            }
        }

        private String getBlockTag4Peer(int peerIndex) {
            return this.testNumber + "_" + peerIndex;
        }

        public void run() {
            String launchTag = LAUNCH_TEST_TAG_PREAMBLE + this.testNumber;

            // produce orchestrator script - sync and collect data
            StringBuilder sb = new StringBuilder();
            SharkNetMessengerAppSupportingDistributedTesting.
                    this.tellUI("WARNING: set timeBomb for orchestrator peer. Adopt this code for longer lasting test scenarios. <<<< WARNING");
            sb.append(CommandNames.CLI_TIME_BOMB + TestLanguageCompiler.CLI_SPACE
                    + MAX_TEST_DURATION_IN_MILLIS*10 + TestLanguageCompiler.LANGUAGE_SEPARATOR);

            sb.append(OrchestratedTestLauncher.scriptStartOrchestrator_SyncWithPeers);
            // wait for each peer to settle
            for(int peerIndex = 0; peerIndex < this.test2run.scripts.size(); peerIndex++) {
                sb.append(CommandNames.CLI_BLOCK);
                sb.append(TestLanguageCompiler.CLI_SPACE);
                // wait for peer until settled
                sb.append(SETTLED_TAG_PREAMBLE);
                sb.append(this.getBlockTag4Peer(peerIndex));
                sb.append(TestLanguageCompiler.CLI_SEPARATOR);
            }
            // tell peers to start test
            sb.append(CommandNames.CLI_RELEASE);
            sb.append(TestLanguageCompiler.CLI_SPACE);
            // release tag - tell peers to start - handshake way 2
            sb.append(launchTag);
            sb.append(TestLanguageCompiler.CLI_SEPARATOR);

            // TODO collect results - will be a single lsMessages in the right channel.

            // finish peer
            sb.append(scriptEnd_Exit);

            String orchestratorScript = sb.toString();

            // produce script for each peer
            String[] effectiveScripts = new String[this.test2run.scripts.size()];
            for(int peerIndex = 0; peerIndex < this.test2run.scripts.size(); peerIndex++) {
                sb = new StringBuilder();
                // set time bomb to avoid orphan processes
                SharkNetMessengerAppSupportingDistributedTesting.
                        this.tellUI("WARNING: set timeBomb in each peer script. Adopt this code for longer lasting test scenarios. <<<< WARNING");
                sb.append(scriptSetTimeBomb);
                // add open connection to orchestrator
                sb.append(scriptStartPeer_SyncWithOrchestator);

                // tell orchestrator settled:
                sb.append(CommandNames.CLI_RELEASE);
                sb.append(TestLanguageCompiler.CLI_SPACE);
                // handshake way 1 - tell orchestrator settled
                sb.append(SETTLED_TAG_PREAMBLE);
                sb.append(this.getBlockTag4Peer(peerIndex));
                sb.append(TestLanguageCompiler.CLI_SEPARATOR);

                // wait until orchestrator tells to launch test - handshake 2
                sb.append(CommandNames.CLI_BLOCK);
                sb.append(TestLanguageCompiler.CLI_SPACE);
                sb.append(launchTag);
                sb.append(TestLanguageCompiler.CLI_SEPARATOR);

                // and better wait a moment
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
                        scriptRunnerProcess = new ScriptRunnerProcess(ORCHESTRATOR_PEER_NAME,
                        Integer.toString(this.testNumber), orchestratorScript);
                scriptRunnerProcess.start();
            } catch (IOException e) {
                String log = "could not start orchestrator process / don't send scripts to peers: " + e.getStackTrace();
                SharkNetMessengerAppSupportingDistributedTesting.this.tellUIError(log);
                Log.writeLogErr(this, log);
                return;
            }

            /*
            ScriptRunnerThread scriptRunnerThread =
                    new ScriptRunnerThread(ORCHESTRATOR_PEER_NAME,
                            Integer.toString(this.testNumber), orchestratorScript);
            SharkNetMessengerAppSupportingDistributedTesting.this.tellUI(
                    "running script as peer " + ORCHESTRATOR_PEER_NAME + ": " + orchestratorScript);
            scriptRunnerThread.start();
             */

            // now - send script to each peer

            // to avoid even the slightest chance of a race condition - make a little break;
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            SharkNetMessengerAppSupportingDistributedTesting.this.tellUI("sending test scripts to peers");
            Log.writeLog(this, "sending test scripts to peers");

            try {
                for (int i = 0; i < this.test2run.peerEnvironment.size(); i++) {
                    PeerHostingEnvironmentDescription peerEnvironment = this.test2run.peerEnvironment.get(i);
                    TestScriptDescription testScriptDescription = new TestScriptDescription(
                            peerEnvironment.toString(), // peer IP Address
                            i, // peerName
                            effectiveScripts[i], // testscript to run
                            this.testNumber,
                            peerEnvironment.peerID
                    );

                    // send message
                    SharkNetMessengerAppSupportingDistributedTesting.this.getSharkMessengerComponent().sendSharkMessage(
                            SharkNetMessengerAppSupportingDistributedTesting.TEST_SCRIPT_FORMAT,
                            testScriptDescription.getMessageBytes(),
                            SharkNetMessengerAppSupportingDistributedTesting.TEST_SCRIPT_CHANNEL, // specific channel
                            peerEnvironment.ipAddress, // ip address as peer address
                            false, // no signing
                            false // no encryption
                    );
                    Log.writeLog(this, "sent script: ip | test# | script"
                            + peerEnvironment.peerID + " | "
                            + this.testNumber + " | "
                            + effectiveScripts[i]);

                    SharkNetMessengerAppSupportingDistributedTesting.this.
                            tellUI("test scripts sent to " + peerEnvironment.peerID + "@" + peerEnvironment.ipAddress);
                }
            }
            catch(IOException | SharkNetMessengerException ioe) {
                SharkNetMessengerAppSupportingDistributedTesting.this.
                        tellUIError("cannot send test scripts / abort test# " + this.testNumber +
                                " / " + ioe.getLocalizedMessage());
            }
        }
    }

    /** launch ready distributed test scenario */
    private void launchTests() throws UnknownHostException {
        this.tellUI("launchTests reached");
        while(!this.orchestratedTestsReady.isEmpty()) {
            OrchestratedTest readyTest = this.orchestratedTestsReady.remove(0);
            OrchestratedTestLauncher readyTestLauncher = new OrchestratedTestLauncher(readyTest);
            readyTestLauncher.start();
        }
    }

    private class TestEnsemble {
        final Integer waitingTestIndex;
        final List<PeerHostingEnvironmentDescription> peerEnvironment;

        TestEnsemble(Integer waitingTestIndex, List<PeerHostingEnvironmentDescription> peerEnvironment) {
            this.waitingTestIndex = waitingTestIndex;
            this.peerEnvironment = peerEnvironment;
        }
    }

    // find fitting peers for any test - return null or waiting test index + list of ipAddresses of fitting peers
    private TestEnsemble findFittingPeers() {
        synchronized (this) {
            // walk through waiting tests - order of their appearance
            int waitingTestIndex = -1;
            for (OrchestratedTest waitingTest : this.orchestratedTestsWaiting) {
                waitingTestIndex++;
                // walk through required environments
                for (PeerHostingEnvironmentDescription requiredEnvironment : waitingTest.peerEnvironment) {
                    // let's look for enough peers to run the test

                    // it is a list... important since position fitting peer same as required environment
                    List<PeerHostingEnvironmentDescription> fittingPeers = new ArrayList<>();

                    // let's walk through available peers to look for a match
                    for (String peerID : this.availablePeers.keySet()) {
                        PeerHostingEnvironmentDescription availableEnvironment = this.availablePeers.get(peerID);
                        // match?
                        boolean match = true;

                        // os required? if so - does available environment match?
                        if (requiredEnvironment.osName != null && !requiredEnvironment.osName.isEmpty()) {
                            if (!requiredEnvironment.osName.equalsIgnoreCase(availableEnvironment.osName)) {
                                match = false;
                            }
                        }
                        // version required? if so - does available environment match?
                        if(match && requiredEnvironment.osVersion != null && !requiredEnvironment.osVersion.isEmpty()) {
                            if (!requiredEnvironment.osVersion.equalsIgnoreCase(availableEnvironment.osVersion)) {
                                match = false;
                            }
                        }

                        // found a fitting peer ?
                        if(match) {
                            // indeed - found one
                            fittingPeers.add(availableEnvironment);
                            if(fittingPeers.size() == waitingTest.peerEnvironment.size()) {
                                // we have all required peers - done here.
                                return new TestEnsemble(waitingTestIndex, fittingPeers);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
