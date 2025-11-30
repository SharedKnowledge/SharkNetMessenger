package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;

import java.io.IOException;
import java.net.InetAddress;

public class UICommandScriptRQ extends AbstractCommandNoParameter {
    SharkNetMessengerAppSupportingDistributedTesting snmTestSupport;

    public UICommandScriptRQ(SharkNetMessengerAppSupportingDistributedTesting sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
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

            // collect information
            ScriptRQMessage scriptRQMessage = new ScriptRQMessage(
                    InetAddress.getLocalHost().getHostAddress(), // IP Adresse
                    System.getProperty("os.name"), // os name
                    System.getProperty("os.version") // os version
            );


            // send message
            messenger.sendSharkMessage(
                    SharkNetMessengerAppSupportingDistributedTesting.PEER_HOST_DESCRIPTION_FORMAT,
                    scriptRQMessage.getMessageBytes(),
                    SharkNetMessengerAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL, // specific channel
                    (CharSequence) null, // no specific receiver
                    false, // no signing
                    false // no encryption
            );
            this.getSharkMessengerApp().tellUI("script request sent: " + scriptRQMessage);
        } catch (SharkException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }

    }

    @Override
    public String getDescription() {
        return "sends a request for test script into channel " +
                SharkNetMessengerAppSupportingDistributedTesting.SCRIPT_RQ_CHANNEL;
    }
}
