package net.sharksystem.ui.messenger.cli.commandarguments;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;

/**
 * An argument for string input
 */
public class UICommandStringArgument extends UICommandArgument<String> {

    public UICommandStringArgument(SharkNetMessengerApp sharkMessengerApp) {
        super(sharkMessengerApp);
    }

    /**
     * @param input The user input.
     * @return false, if the input is empty
     */
    @Override
    public boolean tryParse(String input) {
        if(super.tryParse(input)) {
            this.parsedInput = input;
            return true;
        }
        return false;
    }
}
