package net.sharksystem.ui.messenger.cli.commandarguments;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;

/**
 * Predefined argument for long input
 */
public class UICommandLongArgument extends UICommandArgument<Long> {

    public UICommandLongArgument(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }
    
    @Override
    public boolean tryParse(String input)  {
        super.setEmptyStringAllowed(false);
        if(super.tryParse(input)) {
            try {
                this.parsedInput = Long.parseLong(input);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
