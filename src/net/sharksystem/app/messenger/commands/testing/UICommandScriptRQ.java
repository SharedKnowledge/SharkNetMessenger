package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.ui.messenger.cli.distributedtesting.SNMAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandNoParameter;

import java.io.IOException;

public class UICommandScriptRQ extends AbstractCommandNoParameter {
    SNMAppSupportingDistributedTesting snmTestSupport;

    public UICommandScriptRQ(SNMAppSupportingDistributedTesting sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                             String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        // need test support
        this.snmTestSupport = sharkMessengerApp;
    }

    @Override
    protected void execute() throws Exception {
        String messageString = null;

        try {
            SharkNetMessengerComponent messenger = this.getSharkMessengerApp().getSharkMessengerComponent();

            this.snmTestSupport.becomeTestPeer();
            this.getSharkMessengerApp().tellUI("published script request");

        } catch (SharkException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }
    }

    @Override
    public String getDescription() {
        return "sends a request for test script into channel " +
                SNMAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL;
    }
}
