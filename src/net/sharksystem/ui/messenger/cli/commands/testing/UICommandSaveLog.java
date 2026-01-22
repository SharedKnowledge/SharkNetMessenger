package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;

/**
 * Command for saving the log in a file.
 */
public class UICommandSaveLog extends UICommand {

    private final UICommandStringArgument fileName;

    public UICommandSaveLog(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                            String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.fileName = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder().
                addQuestion("File path: ", this.fileName).
                build();
    }

    @Override
    public void execute() throws Exception {
        File file = new File(this.fileName.getValue());
        if (file.createNewFile()) {
            try {
                List<String> commandHistory = this.getSharkMessengerUI().getCommandHistory();
                PrintWriter pw = new PrintWriter(file);
                for (String command : commandHistory) {
                    pw.println(command);
                }
                pw.close();
            } catch (IOException e) {
                getSharkMessengerUI().printError("Couldn't write to or create file" + fileName);
            }
        } else {
            getSharkMessengerUI().printError("Won't write to already existing file");
        }
    }

    @Override
    public String getDescription() {
        return "Saves the current log to a file.";
    }

    /**
     * @param arguments in following order:
     * <ol>
     *  <li>fileName - String</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            return false;
        }

        boolean isParsable = fileName.tryParse(arguments.get(0));

        return isParsable;
    }

}
