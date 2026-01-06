package net.sharksystem.ui.messenger.cli;

import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessagesReceivedListener;
import net.sharksystem.app.messenger.SharkNetMessengerException;

import java.io.IOException;

public class SNMMessageReceivedListener extends SharkNetMessengerAppListener implements SharkNetMessagesReceivedListener {
    public SNMMessageReceivedListener(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }

    @Override
    public void sharkMessagesReceived(CharSequence uri) {
        try {
            SharkNetMessageList messages = this.sharkMessengerApp.getSharkMessengerComponent().getChannel(uri).getMessages();
            String sb = messages.size() +
                    " message(s) received in channel " +
                    uri;
            this.sharkMessengerApp.tellUI(sb);

        } catch (SharkNetMessengerException | IOException e) {
            this.sharkMessengerApp.tellUIError("exception when receiving messages:" + e.getLocalizedMessage());
        }
    }
}
