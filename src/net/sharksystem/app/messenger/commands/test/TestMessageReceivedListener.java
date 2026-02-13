package net.sharksystem.app.messenger.commands.test;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.app.messenger.SharkNetMessagesReceivedListener;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a listener specificially for testing purposes.
 */
public class TestMessageReceivedListener implements SharkNetMessagesReceivedListener {
    private final SharkNetMessengerApp sharkMessengerApp;
    private static TestMessageReceivedListener instance;

    // <channelURI, <messageID, timeStamp>>
    private final Map<CharSequence, Map<Integer, Long>> receivedMessages;

    /**
     * Create the singleton only this way, if you are sure that this is the
     * only time it is created.
     * @param sharkMessengerApp
     */
    public TestMessageReceivedListener(SharkNetMessengerApp sharkMessengerApp) {
        this.sharkMessengerApp = sharkMessengerApp;
        this.receivedMessages = new HashMap<>();
        instance = this;
    }

    /**
     * This class is a pseudo singleton. Meaning creation through the
     * constructor is possible but should be avoided at all cost. Use only this
     * method to access the instance.
     * @return the singleton instance.
     */
    public static TestMessageReceivedListener getInstance() {
        if (instance == null) {
            System.out.println("Not initialized!");
        }
        return instance;
    }

    /**
     * When a new or multiple new messages are received, an id will be assigned
     * to each individual message.
     */
    @Override
    public void sharkMessagesReceived(CharSequence uri) {
        int messageCounter = 0;

        if (receivedMessages.containsKey(uri)){
            messageCounter = receivedMessages.get(uri).size();
        } else {
            receivedMessages.put(uri, new HashMap<>());
        }

        try {
            SharkNetMessageList messages = this.sharkMessengerApp.getSharkMessengerComponent().getChannel(uri).getMessages();

            // When multiple messages are received as a block (possible when connection
            // reestablished) they should receive the same time stamp.
            long currentTime = System.currentTimeMillis();
            for (int i = messages.size()-1; i >= messageCounter; i--){
                SharkNetMessage message = messages.getSharkMessage(i, true);
                int id = getIDFromContent(message.getContent());
                receivedMessages.get(uri).put(id, currentTime);
            }
        } catch (SharkNetMessengerException | IOException e) {
            System.out.println("TODO: exception in MessageReceivedListener:" + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (ASAPSecurityException e) {
            System.out.println("TODO: ASAPSecurityException in MessageReceivedListener:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the id from a test message. By default, when using the test
     * commands an id should be created as part of the message content.
     * @param content
     * @return the id of the content or a value < 0 if no id could be extracted.
     */
    private int getIDFromContent(byte[] content) {
        int id = -1;
        String contentString = new String(content, StandardCharsets.UTF_8);
        try {
            id = Integer.parseInt(contentString.split(System.lineSeparator())[0]);
        } catch (NumberFormatException e) {
            System.err.println("Test-Message had no id to extract.");
            e.printStackTrace();
        }
        return id;
    }

    public long getReceivedTime(CharSequence uri, int messageID) {
        return receivedMessages.get(uri).get(messageID);
    }

    /**
     * Get the received message count for a specific channel for this peer.
     * @param uri of the channel.
     * @return the amount of received messages in the channel.
     */
    public int getMessageCount(CharSequence uri) {
        if(!receivedMessages.containsKey(uri)) {
            return 0;
        }
        return receivedMessages.get(uri).size();
    }

    /**
     * Get the total received message count for this peer.
     * @return the amount of received messages.
     */
    public int getMessageCount() {
        return receivedMessages.entrySet().stream()
        .map(x-> x.getValue().size())
        .reduce(0, Integer::sum);
    }
}
