package net.sharksystem.ui.messenger.cli;

import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessagesReceivedListener;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SNMMessageReceivedListener extends SharkNetMessengerAppListener implements SharkNetMessagesReceivedListener {
    public SNMMessageReceivedListener(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }

    @Override
    public void sharkMessagesReceived(CharSequence uri) {
        try {
            SharkNetMessageList messages = this.sharkMessengerApp.getSharkMessengerComponent().getChannel(uri).getMessages();
            String sb = messages.size() +
                    "messages received in channel " +
                    uri;
            this.sharkMessengerApp.tellUI(sb);

            // if release label received
            if(uri.toString().equalsIgnoreCase(SharkNetMessengerApp.TEST_BLOCK_RELEASE_CHANNEL.toString())) {
                List<String> releaseLabels = new ArrayList<>();
                for(int i = 0; i < messages.size(); i++) {
                    SharkNetMessage sharkMessage = messages.getSharkMessage(i, true);
                    CharSequence csLabel = SerializationHelper.bytes2characterSequence(sharkMessage.getContent());
                    releaseLabels.add(csLabel.toString());
                }
                this.sharkMessengerApp.releaseReceived(releaseLabels);
            }

        } catch (SharkNetMessengerException | IOException | ASAPException e) {
            this.sharkMessengerApp.tellUIError("exception when receiving messages:" + e.getLocalizedMessage());
        }
    }
}
