package net.sharksystem.app.messenger.commands.basics;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandNoParameter;

public class UICommandHelp extends AbstractCommandNoParameter {
    public UICommandHelp(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                         String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    public void execute() throws Exception {
        this.getSharkMessengerUI().printUsage();
    }

    @Override
    public String getDescription() {
        return "show valid commands.";
    }
}