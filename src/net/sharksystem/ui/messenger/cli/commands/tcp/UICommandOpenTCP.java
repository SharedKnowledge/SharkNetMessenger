package net.sharksystem.ui.messenger.cli.commands.tcp;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleInteger;

import java.io.IOException;

/**
 * This command opens a port for a peer to connect to over TCP/IP.
 */
public class UICommandOpenTCP extends AbstractCommandWithSingleInteger {

    public UICommandOpenTCP(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                            String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        try {
            this.getSharkMessengerApp().openTCPConnection(this.getIntegerArgument());
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
