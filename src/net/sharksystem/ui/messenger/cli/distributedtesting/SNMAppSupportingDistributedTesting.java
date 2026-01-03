package net.sharksystem.ui.messenger.cli.distributedtesting;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.*;

public class SNMAppSupportingDistributedTesting extends SharkNetMessengerApp {
    public static final CharSequence TEST_BLOCK_RELEASE_CHANNEL = "snm://block_release";
    public static final CharSequence SCRIPT_RQ_CHANNEL = "snm://scriptRQ";
    public static final String PEER_HOST_DESCRIPTION_FORMAT = "snm/peerHostDesc";
    public static final String TEST_SCRIPT_FORMAT = "snm/testScript";
    public static final CharSequence TEST_SCRIPT_CHANNEL = "snm://testScripts";

    private PeerHostingEnvironmentDescription myEnvironment;

    String getLocalIPAddress() throws UnknownHostException {
        if(this.myEnvironment == null) this.myEnvironment = new PeerHostingEnvironmentDescription(this.getPeerName());
        return this.myEnvironment.ipAddress;
    }

    public SNMAppSupportingDistributedTesting(String peerName, PrintStream out, PrintStream err)
            throws SharkException, IOException {
        this(peerName, 60*10, out, err);
    }

    public SNMAppSupportingDistributedTesting(String peerName, int syncWithOthersInSeconds, PrintStream out, PrintStream err)
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

        /* would be better that way
        this.getSharkPeer().getASAPPeer().sendTransientASAPMessage(
                SNMAppSupportingDistributedTesting.PEER_HOST_DESCRIPTION_FORMAT,
                SNMAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL, // specific channel
                scriptRQMessage.getMessageBytes()
        );
         */

        // send message
        this.getSharkMessengerComponent().sendSharkMessage(
                SNMAppSupportingDistributedTesting.PEER_HOST_DESCRIPTION_FORMAT,
                scriptRQMessage.getMessageBytes(),
                SNMAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL, // specific channel
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
        synchronized (SNMAppSupportingDistributedTesting.this) {
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
                        // this is script is for me - alright. Is it a copy?
                        this.tellUI("test script for me received - prepare script runner" + testScriptDescription);
                        Log.writeLog(this, "test script for me received - prepare script runner"
                                + testScriptDescription);

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
                                testScriptDescription.testID,
                                testScriptDescription.script).start();
                        this.tellUI("running script: " + testScriptDescription.script);
                        Log.writeLog(this, "script running, remember to avoid redo it: " + testScriptDescription);
                        this.handledScripts.add(testScriptDescription);
                    }
                    else {
                        this.tellUI("test script received, not for me though.");
                        Log.writeLog(this, "test script received, not for me: " + testScriptDescription);
                    }
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
        synchronized (SNMAppSupportingDistributedTesting.this) {
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
//                    for (int rqIndex = this.lastScriptRQIndex + 1; rqIndex < rqMessages.size(); rqIndex++) {
                    for (int rqIndex = 0; rqIndex < rqMessages.size(); rqIndex++) {
                        this.lastScriptRQIndex = rqIndex; // update each round - before possible exceptions

                        SharkNetMessage rqSharkMessage = rqMessages.getSharkMessage(rqIndex, false);
                        PeerHostingEnvironmentDescription peerHostDescription =
                                new PeerHostingEnvironmentDescription(rqSharkMessage.getContent());

                        // add or replace information
                        this.availablePeers.put(peerHostDescription.peerID, peerHostDescription);
                        String log = "added / updated volunteering peer: " + peerHostDescription;
                        this.tellUI(log);
                        Log.writeLog(this, log);
                    }
                    // try to set up a test(s).
                    this.stageTests();
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
                // new ready test created
                this.orchestratedTestsReady.add(readyTest);

                ////////////// update waiting tests list and available peers
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

    /** launch ready distributed test scenario */
    private void launchTests() throws UnknownHostException {
        this.tellUI("launchTests reached");
        while(!this.orchestratedTestsReady.isEmpty()) {
            OrchestratedTest readyTest = this.orchestratedTestsReady.remove(0);
            OrchestratedTestLauncher readyTestLauncher = new OrchestratedTestLauncher(this, readyTest);
            readyTestLauncher.start();
        }
    }

    /**
     * find fitting peers for any test - return null or waiting test index + list of ipAddresses of fitting peers
     * does not change member - it's just a proposal yet.
     * @return possible test ensemble
     */
    private TestEnsemble findFittingPeers() {
        synchronized (this) {
            // first - make a copy of all available peers to avoid placing them twice
            List<PeerHostingEnvironmentDescription> freePeersList = new ArrayList<>(this.availablePeers.values());

            // walk through waiting tests - order of their appearance
            int waitingTestIndex = -1;
            for (OrchestratedTest waitingTest : this.orchestratedTestsWaiting) {
                waitingTestIndex++;

                // would that be in general possible?
                if(waitingTest.peerEnvironment.size() > freePeersList.size()) {
                    // we don't have enough peers for this test
                    continue;
                }

                // create a list. try to place a free peer to each position.
                List<PeerHostingEnvironmentDescription> fittingPeers = new ArrayList<>();

                // walk through required environments and find a match

                // use a while loop to make explicitly clear that we deal with a list and its order matters
                int index = 0;
                boolean stillAChance = true;
                while(stillAChance && index < waitingTest.peerEnvironment.size()) {
                    PeerHostingEnvironmentDescription requiredEnvironment = waitingTest.peerEnvironment.get(index++);
                    // let's look for at leat one peer that can run this test

                    // let's walk through free peers to look for a match
                    boolean foundMatch = false;
                    for (PeerHostingEnvironmentDescription freeEnvironment : freePeersList) {
                        // found a fitting peer ?
                        if(PeerHostingEnvironmentDescription.match(requiredEnvironment, freeEnvironment)) {
                            // indeed - found one
                            fittingPeers.add(freeEnvironment);
                            freePeersList.remove(freeEnvironment);
                            foundMatch = true;
                            break; // found a match - go ahead in requirements list
                        }
                    }
                    if(!foundMatch) stillAChance = false;
                } // while
                if(stillAChance) {
                    // means - we have a peer for each position
                    return new TestEnsemble(waitingTestIndex, fittingPeers);
                } else {
                    // we cannot stage this test - put back allocated peers
                    freePeersList.addAll(fittingPeers);
                }
            } // for - walking through waiting test cases
        }
        return null;
    }
}
