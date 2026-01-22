package net.sharksystem.ui.messenger.cli.commands.test;

/**
 * This class contains all information about a sent message that are used in the verification process of a
 * distributed test.
 */
class MessageData {
    public final String receiver;
    public final String channelUri;
    public final String content;
    public final int messageID;

    public MessageData(String receiver, String channelUri, String content, int messageID) {
        this.receiver = receiver;
        this.channelUri = channelUri;
        this.content = content;
        this.messageID = messageID;
    }

}
