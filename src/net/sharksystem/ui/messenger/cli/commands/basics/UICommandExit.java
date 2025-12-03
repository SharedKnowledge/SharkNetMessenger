package net.sharksystem.ui.messenger.cli.commands.basics;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;

/**
 * Command for terminating the messenger.
 */
public class UICommandExit extends AbstractCommandNoParameter {
    public UICommandExit(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                         String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    public void execute() throws Exception {
        //this.printTODOReimplement();
        this.getSharkMessengerApp().tellUI("end CLI for peer " +
            this.getSharkMessengerApp().getSharkPeer().getPeerID());
        System.exit(0);
    }

    @Override
    public String getDescription() {
        return "Terminates the messenger.";
    }
}
