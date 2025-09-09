package net.sharksystem.ui.messenger.cli.commands.messenger;

import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleInteger;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

/**
 * This command removes a channel from the peers known channels.
 */
public class UICommandRemoveChannelByIndex extends AbstractCommandWithSingleInteger {
    public UICommandRemoveChannelByIndex(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                         String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    public void execute() throws Exception {
        this.getSharkMessengerApp().tellUI("not yet implemented; not supported in version 1 anyway.");
        /*
        // we start with 1 in UI
        int channelIndex = this.getIntegerArgument() - 1;
        SharkNetMessengerComponent sharkMessengerComponent = this.getSharkMessengerApp().getSharkMessengerComponent();
        CharSequence uri = sharkMessengerComponent.getChannel(channelIndex).getURI();
        sharkMessengerComponent.removeChannel(uri);
        this.getSharkMessengerApp().tellUI("channel removed - index most probably changed");
         */
    }

    @Override
    public String getDescription() {
        return "Removes a channel by index (not supported in version 1. not implemented yet.";
    }
}
