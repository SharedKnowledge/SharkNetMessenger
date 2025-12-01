package net.sharksystem.ui.messenger.cli;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.ui.messenger.cli.commands.testing.PeerHostingEnvironmentDescription;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class SharkNetMessengerAppSupportingDistributedTesting extends SharkNetMessengerApp {
    public static final CharSequence TEST_BLOCK_RELEASE_CHANNEL = "snm://block_release";
    public static final CharSequence SCRIPT_RQ_CHANNEL = "snm://scriptRQ";
    public static final String PEER_HOST_DESCRIPTION_FORMAT = "snm/peerHostDesc";

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
        return super.produceStringForMessage(contentType, content);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                     block / release                                     //
    /////////////////////////////////////////////////////////////////////////////////////////////
    private Set<String> receivedLabels = new HashSet<>(); // more instances of the same label has no specific semantics
    private List<Thread> blockedThreads = new ArrayList<>(); // a thread can wait for more labels - useful or not
    public void block(String label) {
        // check if already released
        while(true) {
            for(String receivedLabel : this.receivedLabels) {
                if (receivedLabel.equalsIgnoreCase(label)) {
                    return; // block released
                }
            }
            try {
                this.blockedThreads.add(Thread.currentThread());
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // new message received - try again
            }
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
    //                                     test orchestration                                  //
    /////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, PeerHostingEnvironmentDescription> availablePeers = new HashMap<>();

    private class OrchestratedTest {
        List<PeerHostingEnvironmentDescription> requiredPeerEnvironment;
        List<String> scripts;
        OrchestratedTest(List<PeerHostingEnvironmentDescription> requiredPeerEnvironment, List<String> scripts) {
            this.requiredPeerEnvironment = requiredPeerEnvironment;
            this.scripts = scripts;
        }
    }

    private List<OrchestratedTest> orchestratedTestsWaiting = new ArrayList<>();
    private List<OrchestratedTest> orchestratedTestsInLaunch = new ArrayList<>();

    public void orchestrateTest(List<PeerHostingEnvironmentDescription> requiredPeerEnvironment, List<String> scripts) {
        this.orchestratedTestsWaiting.add(new OrchestratedTest(requiredPeerEnvironment, scripts));
    }

    private int lastScriptRQIndex = -1;
    public void scriptRQReceived(CharSequence scriptRQChannel) {
        this.tellUI("ScriptRQ reached");
        synchronized (this) {
            try {
                SharkNetMessageList rqMessages =
                        this.getSharkMessengerComponent().getChannel(scriptRQChannel).getMessages();
                for (int rqIndex = this.lastScriptRQIndex + 1; rqIndex < rqMessages.size(); rqIndex++) {
                    this.lastScriptRQIndex = rqIndex; // update each round - before possible exceptions

                    SharkNetMessage rqSharkMessage = rqMessages.getSharkMessage(rqIndex, true);
                    PeerHostingEnvironmentDescription peerHostDescription =
                            new PeerHostingEnvironmentDescription(rqSharkMessage.getContent());

                    // add or replace information
                    this.availablePeers.put(peerHostDescription.ipAddress, peerHostDescription);

                    // try to set up a test(s).
                    this.stageTests();
                }
            } catch (SharkNetMessengerException | IOException | ASAPException e) {
                this.tellUIError("problems handling script RQ channel: " + e.getLocalizedMessage());
            }
        }
    }

    private void stageTests() {
        TestEnsemble testEnsemble = null;
        do {
            testEnsemble = this.findFittingPeers();
            if (testEnsemble != null) {
                // we found an ensemble to run that test
                this.orchestratedTestsInLaunch.add(
                        this.orchestratedTestsWaiting.get(testEnsemble.waitingTestIndex));

                // TODO - does not work - why?
                this.orchestratedTestsWaiting.remove(testEnsemble.waitingTestIndex);

                // make peers unavailable
                for(int i = 0; i < testEnsemble.peerIPAddresses.length; i++) {
                    this.availablePeers.remove(testEnsemble.peerIPAddresses[i]);
                }
            }
        } while(testEnsemble != null); // do again until there is not further match

        if(!this.orchestratedTestsInLaunch.isEmpty()) {
            // launch tests
            System.out.println("TODO: launch tests");
        }
    }

    private class TestEnsemble {
        final Integer waitingTestIndex;
        final String[] peerIPAddresses;

        TestEnsemble(Integer waitingTestIndex, String[] peerIPAddresses) {
            this.waitingTestIndex = waitingTestIndex;
            this.peerIPAddresses = peerIPAddresses;
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
                for (PeerHostingEnvironmentDescription requiredEnvironment : waitingTest.requiredPeerEnvironment) {
                    // we need exactly that number of peers - underlined by using an array.
                    String[] fittingPeers = new String[waitingTest.requiredPeerEnvironment.size()];
                    int fittingPeerIndex = 0;

                    // let's walk through available peers to look for a match
                    for (String ipAdress : this.availablePeers.keySet()) {
                        PeerHostingEnvironmentDescription availableEnvironment = this.availablePeers.get(ipAdress);
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
                            fittingPeers[fittingPeerIndex++] = ipAdress;
                            if(fittingPeerIndex == fittingPeers.length) {
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
