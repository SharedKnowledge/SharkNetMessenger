package net.sharksystem.app.messenger.commands.hubmanagement;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleInteger;

import java.io.IOException;

public class UICommandStopHub extends AbstractCommandWithSingleInteger {
    public UICommandStopHub(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                            String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        try {
            this.getSharkMessengerApp().stopHub(this.getIntegerArgument());
        } catch (IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }
    }

    @Override
    public String getDescription() {
        // append hint for how to use
        return "Open new port for establishing TCP connections with."
                // append hint for how to use
                ;
    }
}