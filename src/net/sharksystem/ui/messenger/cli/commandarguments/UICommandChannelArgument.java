package net.sharksystem.ui.messenger.cli.commandarguments;

import net.sharksystem.SharkException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;

import java.io.IOException;

/**
 * Predefined argument for a SharkMessengerChannel retrieved through its uri and a peer
 */
public class UICommandChannelArgument extends UICommandArgument<SharkNetMessengerChannel> {

    public UICommandChannelArgument(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }

    @Override
    public boolean tryParse(String input)  {
        super.setEmptyStringAllowed(false);
        if(super.tryParse(input)) {
            try {
                this.parsedInput = this.getSharkMessengerApp().getSharkMessengerComponent().getChannel(input);
                return true;
            } catch (SharkException | IOException e) {
                return false;
            }
        }
        return false;
    }
}
