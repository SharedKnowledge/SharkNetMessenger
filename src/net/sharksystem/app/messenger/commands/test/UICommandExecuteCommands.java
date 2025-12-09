package net.sharksystem.app.messenger.commands.test;

import java.io.InputStreamReader;
import java.util.List;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandBooleanArgument;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;

/**
 * This command starts executing all commands given by a String
 * or File. The commands will be sequentialy executed in order.
 */
public class UICommandExecuteCommands extends UICommand {

    private final UICommandBooleanArgument inSteps;

    public UICommandExecuteCommands(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                    String identifier, boolean rememberCommand) {

        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        inSteps = new UICommandBooleanArgument(sharkMessengerApp);
    }

    /**
     * @param arguments in following order:
     * <ol>
     *  <li>inSteps - boolean [If true, every other command execution
     *                         waits for the user to press Enter before
     *                         performing the next command.]</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            return false;
        }

        boolean isParsable = this.inSteps.tryParse(arguments.get(0));
        return isParsable;
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
    }

    @Override
    protected void execute() throws Exception {
        if (this.inSteps.getValue()) {
            executeInSteps();
            return;
        }

        SharkNetMessengerUI smUI = this.getSharkMessengerUI();
        List<String> commands = smUI.getParsedCommands();

        for (String command : commands) {
            smUI.handleUserInput(command);
        }
    }

    private void executeInSteps() throws Exception {
        InputStreamReader isr = new InputStreamReader(System.in);

        SharkNetMessengerUI smUI = this.getSharkMessengerUI();
        List<String> commands = smUI.getParsedCommands();

        for (String command : commands) {
            System.out.println("Execute with enter: " + command);
            isr.read();
            smUI.handleUserInput(command);
        }
    }

    @Override
    public String getDescription() {
        return "Executes a loaded test script.";
    }
    
}
