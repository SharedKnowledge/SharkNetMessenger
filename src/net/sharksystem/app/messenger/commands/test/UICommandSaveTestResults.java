package net.sharksystem.app.messenger.commands.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandStringArgument;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;

/**
 * This command produces two csv verification files used for evaluating the test
 * results. One for all sent and one for all received messages.
 */
public class UICommandSaveTestResults extends UICommand {
    private final String FILE_EXTENSION = ".csv";
    private final String SEND_SEPERATOR = "_tx_";
    private final String RECEIVED_SEPERATOR = "_rx_";

    private final CharSequence peerName;
    private final UICommandStringArgument testID;

    public UICommandSaveTestResults(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                    String identifier, boolean rememberCommand) throws SharkException {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.peerName = sharkMessengerApp.getSharkPeer().getPeerID();
        this.testID = new UICommandStringArgument(sharkMessengerApp);


    }

    /**
     * @param arguments in following order:
     * <ol>
     *  <li>testID - String</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.isEmpty()) {
            return false;
        }
        return this.testID.tryParse(arguments.get(0));
    }

    @Override
    protected void execute() throws IOException, SharkNetMessengerException, ASAPException {
        String sendFilePath = this.testID.getValue() + SEND_SEPERATOR + peerName + FILE_EXTENSION;
        String receivedFilePath = this.testID.getValue() + RECEIVED_SEPERATOR + peerName + FILE_EXTENSION;

        writeSentMessagesToFile(sendFilePath);
        writeReceivedMessagesToFile(receivedFilePath);
    }

    private void writeSentMessagesToFile(String pathName) throws IOException {
        Path path = Paths.get(pathName);
        Files.write(path, "sender,receiver,uri,id".getBytes());

        List<MessageData> allSentMessageData = SentMessageCounter.getInstance(this.peerName).getSentMessageData();
        for (MessageData messageData : allSentMessageData) {
            String sender = this.peerName.toString();
            String receiver = messageData.receiver;
            String uri = messageData.channelUri;
            int id = messageData.messageID;

            Files.write(path, (System.lineSeparator() + sender + "," + receiver + "," + uri + "," + id)
                    .getBytes(), StandardOpenOption.APPEND);
        }
    }

    private void writeReceivedMessagesToFile(String pathName)
            throws IOException, SharkNetMessengerException, ASAPException {

        final String HEADER = "sender,receiver,uri,id,creationTime,receivedTime";

        Path path = Paths.get(pathName);
        Files.write(path, HEADER.getBytes());

        SharkNetMessengerComponent messenger = super.getSharkMessengerApp().getSharkMessengerComponent();
        List<CharSequence> uris = messenger.getChannelUris();
        TestMessageReceivedListener listener = TestMessageReceivedListener.getInstance();

        for (CharSequence uri : uris) {
            String channelHeader = System.lineSeparator() + "Listener : Total messages for "+ uri +": "+ listener.getMessageCount(uri);
            Files.write(path, channelHeader.getBytes(), StandardOpenOption.APPEND);
            
            SharkNetMessageList messageList = messenger.getChannel(uri).getMessages();
            for (int i = 0; i < messageList.size(); i++) {
                SharkNetMessage message = messageList.getSharkMessage(i, true);
                int messageID = Integer.parseInt(getIDFromContent(message.getContent()));
                // Write only messages where this peer is a recipient.
                if (message.getRecipients().contains(this.peerName)) {
                    String sb = System.lineSeparator() +
                            message.getSender() +
                            "," +
                            this.peerName +
                            "," +
                            uri +
                            "," +
                            messageID +
                            "," +
                            message.getCreationTime() +
                            "," +
                            listener.getReceivedTime(uri, messageID);
                    Files.write(path, sb.getBytes(), StandardOpenOption.APPEND);
                    // TODO: For tests with more than two peers and messages with more recipients or broadcasts this
                    // format might not work for verification since every combination of recipients would need an own
                    // ID count.
                }
            }
        }
    }

    private String getIDFromContent(byte[] content) {
       String contentString = new String(content, StandardCharsets.UTF_8);
       return contentString.split(System.lineSeparator())[0];
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
    }

    @Override
    public String getDescription() {
        return "Writes two " + FILE_EXTENSION + " files with information about all sent and received messages. Use for test verification.";
    }

}
