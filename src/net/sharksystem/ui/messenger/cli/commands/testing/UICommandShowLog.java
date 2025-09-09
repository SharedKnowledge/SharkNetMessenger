package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import java.util.List;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;

/**
 * Command for displaying the log history to the user.
 */
public class UICommandShowLog extends UICommand {

    public UICommandShowLog(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                            String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return null;
    }

    @Override
    public void execute() throws Exception {
        List<String> commandHistory = this.getSharkMessengerUI().getCommandHistory();
        StringBuilder sb = new StringBuilder();
        for (String command : commandHistory) {
            sb.append(command);
            sb.append(System.lineSeparator());
        }
        getSharkMessengerUI().getOutStream().println(sb);
    }

    @Override
    public String getDescription() {
        return "Prints a log message with all executed commands in the right order.";
    }

    /**
     * This command requires no arguments.
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        return true;
    }

}
