package net.sharksystem.ui.messenger.cli.commandarguments;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;

/**
 * An argument for CLICCommands. It has the logic for parsing any user input (String) to the wished object type
 * specified. This separates the parsing from user input from other classes and preserves duplication of parsing code.
 * @param <T> The type of object you want to parse from the user input
 */
public abstract class UICommandArgument<T> {

    private final SharkNetMessengerApp sharkMessengerApp;

    UICommandArgument(SharkNetMessengerApp sharkMessengerApp) {
        this.sharkMessengerApp = sharkMessengerApp;
    }

    protected SharkNetMessengerApp getSharkMessengerApp() {
        return this.sharkMessengerApp;
    }

    private boolean isEmptyStringAllowed;

    /**
     * The parsed input from type T
     */
    protected T parsedInput;

    /**
     * @return the parsed input from type T
     */
    public T getValue() {
        return this.parsedInput;
    }

    public void setValue(T value) {
        this.parsedInput = value;
    }

    /**
     * This function does the parsing and sets the parsed input if parsing was successful.
     * Always call super when overwriting in own class
     * @param input The user input.
     * @return true, if parsing was possible; false otherwise
     */
    public boolean tryParse(String input) {
        //if empty string is allowed, the input is always fine.
        //Otherwise, the input can't be "" but must be something else.
        return this.isEmptyStringAllowed || !input.equals("");
    }

    /**
     * Set if an empty input result in a successful parse.
     * The default is false.
     * @param isAllowed if the empty string is a correct value to be entered by the user
     */
    public void setEmptyStringAllowed(boolean isAllowed) {
        this.isEmptyStringAllowed = isAllowed;
    }
}
