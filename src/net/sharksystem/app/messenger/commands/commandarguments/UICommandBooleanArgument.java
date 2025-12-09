package net.sharksystem.app.messenger.commands.commandarguments;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;

/**
 * Predefined argument for boolean
 */
public class UICommandBooleanArgument extends UICommandArgument<Boolean> {
    public UICommandBooleanArgument(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }

    @Override
    public boolean tryParse(String input)  {
        super.setEmptyStringAllowed(false);
        if(super.tryParse(input)) {
            this.parsedInput = Boolean.parseBoolean(input);
            return true;
        } else {
            return false;
        }
    }
}
