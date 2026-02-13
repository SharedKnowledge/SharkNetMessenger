package net.sharksystem.app.messenger.commands.test;

import java.util.*;

/**
 * This class serves testing purposes. It gives ID's to and saves information about all sent messages.
 */
public class SentMessageCounter {
    private static final Map<CharSequence, SentMessageCounter> instances = new HashMap<>();
    private final Map<String, Integer> sentMessageIDs = new HashMap<>();
    private final List<MessageData> sentMessageData = new ArrayList<>();

    /**
     * Singleton like access to a SentMessageCounter instance bound to a CharacterSequence. This mechanism is necessary 
     * for correctly working unit tests on verification files because every peer needs his own Counter.
     * @param peerName
     * @return Instance of the Counter for a specific peer.
     */
    public static SentMessageCounter getInstance(CharSequence peerName) {
        if (instances.get(peerName) == null) {
            instances.put(peerName, new SentMessageCounter());
        }
        return instances.get(peerName);
    }

    private SentMessageCounter() {}

    /**
     * This method saves all information of a message this peer is about to send and increments the message counter.
     * @param receiver
     * @param channelUri
     * @param content
     * @return An ID for this message which is a consecutive number for the combination of recipient and channel.
     */
    public int nextMessage(String receiver, String channelUri, String content) {
        String counterKey = receiver + "_" + channelUri;
        if (!this.sentMessageIDs.containsKey(counterKey)) {
            this.sentMessageIDs.put(counterKey, 0);
        }
        int messageID = this.sentMessageIDs.get(counterKey);
        this.sentMessageIDs.put(counterKey, ++messageID);
        this.sentMessageData.add(new MessageData(receiver, channelUri, content, messageID));
        return messageID;
    }

    public int getMessageCount(String receiver) {
        return this.sentMessageIDs.get(receiver);
    }

    public void resetMessageCounter() {
        this.sentMessageIDs.clear();
        this.sentMessageData.clear();
    }

    public List<MessageData> getSentMessageData() {
        return this.sentMessageData;
    }

}
