package net.sharksystem.ui.messenger.cli.commands.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleInteger;
import net.sharksystem.app.messenger.SharkNetMessageList;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import java.io.IOException;

/**
 * This command lists all known messages of a channel.
 */
public class UICommandListMessages extends AbstractCommandWithSingleInteger {
    public static final int DEFAULT_CHANNEL_INDEX = 1;

    public UICommandListMessages(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                 String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand, true, DEFAULT_CHANNEL_INDEX);
    }

    @Override
    public void execute() throws Exception {
        try {
            int userChannelIndex = this.getIntegerArgument(); // we start with 1 in UI and 0 inside
            int channelIndex = this.getIntegerArgument() - 1; // we start with 1 in UI and 0 inside
            SharkNetMessengerComponent messenger = this.getSharkMessengerApp().getSharkMessengerComponent();
            SharkNetMessengerChannel channel = null;
            try {
                channel = messenger.getChannel(channelIndex);
            }
            catch (SharkException se) {
                this.getSharkMessengerApp().tellUI("there is no channel index " + userChannelIndex);
                return;
            }
            SharkNetMessageList messages = channel.getMessages();
            if(messages == null || messages.size() <1) {
                this.getSharkMessengerApp().tellUI("no messages in channel " + userChannelIndex);
                return;
            }
            ChannelPrinter channelPrinter = new ChannelPrinter();
            this.getSharkMessengerApp().tellUI(ChannelPrinter.getChannelDescription(channel));

            this.getSharkMessengerApp().tellUI(
                    channelPrinter.getMessagesASString(this.getSharkMessengerApp(),
                            this.getSharkMessengerApp().getSharkPKIComponent(),
                            channel.getURI().toString(), messages));
        } catch (SharkException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }
    }

    @Override
    public String getDescription() {
        return "List messages in channel (index default: 1)";
    }
}
