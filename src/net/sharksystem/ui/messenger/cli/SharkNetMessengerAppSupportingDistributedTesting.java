package net.sharksystem.ui.messenger.cli;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.ui.messenger.cli.commands.testing.ScriptRQMessage;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                return new ScriptRQMessage(content).toString();
            } catch (IOException | ASAPException e) {
                return "known format - malformed content";
            }
        }
        return super.produceStringForMessage(contentType, content);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                                       test support                                      //
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
}
