package net.sharksystem.app.messenger.commands.commandarguments;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;

/**
 * Predefined argument for integer input
 */
public class UICommandIntegerArgument extends UICommandArgument<Integer> {
    
    public UICommandIntegerArgument(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }

    @Override
    public boolean tryParse(String input)  {
        super.setEmptyStringAllowed(false);
        if(super.tryParse(input)) {
            try {
                this.parsedInput = Integer.parseInt(input);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
