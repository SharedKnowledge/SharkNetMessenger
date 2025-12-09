package net.sharksystem.app.messenger.commands.test;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;

import java.util.List;

/**
 * This command resets all IDs and information about sent messages. Use this
 * when running multiple tests without interruptions. But make sure to save
 * the test results before resetting.
 */
public class UICommandResetMessageCounter extends UICommand {
    private final String sender;
    private final String peerName;

    /**
     * Creates a command object.
     *
     * @param sharkMessengerApp
     * @param sharkMessengerUI
     * @param identifier        The identifier of the command.
     * @param rememberCommand   If the command should be saved in the history log.
     */
    public UICommandResetMessageCounter(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI, String identifier, boolean rememberCommand) throws SharkException {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.sender = sharkMessengerApp.getSharkPeer().getPeerID().toString();
        this.peerName = sharkMessengerApp.getSharkPeer().getPeerID().toString();
    }

    /**
     * This command requires no arguments.
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        return true;
    }

    @Override
    protected void execute() throws Exception {
        SentMessageCounter.getInstance(this.peerName).resetMessageCounter();
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
    }

    @Override
    public String getDescription() {
        return "Resets all saved information about sent messages in SentMessageCounter. Use between test runs.";
    }
}
