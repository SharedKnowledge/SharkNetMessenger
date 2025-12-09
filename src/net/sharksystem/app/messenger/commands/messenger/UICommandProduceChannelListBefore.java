package net.sharksystem.app.messenger.commands.messenger;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.app.messenger.SharkNetMessengerException;

import java.io.IOException;

public abstract class UICommandProduceChannelListBefore extends UICommand {
    public UICommandProduceChannelListBefore(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                             String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    protected void runBefore() {
        // produce channel list
        try {
            SharkNetMessengerComponent messengerComponent = this.getSharkMessengerApp().getSharkMessengerComponent();
            ChannelPrinter.getChannelDescriptions(messengerComponent);
        } catch (IOException | SharkNetMessengerException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }
    }
}
