package net.sharksystem.ui.messenger.cli.commands.tcp;

import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleInteger;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import java.io.IOException;

/**
 * Close a TCP connection to another peer.
 */
public class UICommandCloseTCP extends AbstractCommandWithSingleInteger {
    public UICommandCloseTCP(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI, String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        try {
            this.getSharkMessengerApp().closeTCPConnection(this.getIntegerArgument());
        } catch (IOException e) {
            // exception is expected - ignore it
            //this.printErrorMessage(e.getLocalizedMessage());
        }
    }

    @Override
    public String getDescription() {
        // append hint for how to use
        return "Closes a specified open port - except no more new connections to be established."
                // append hint for how to use
                ;
    }
}
