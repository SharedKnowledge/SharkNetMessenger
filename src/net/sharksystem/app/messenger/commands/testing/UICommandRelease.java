package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.ui.messenger.cli.distributedtesting.SNMAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UICommandRelease extends AbstractCommandWithSingleString {
    SNMAppSupportingDistributedTesting snmTestSupport;

    public UICommandRelease(SNMAppSupportingDistributedTesting sharkMessengerApp, SharkNetMessengerUI smUI, String wait, boolean b) {
        super(sharkMessengerApp, smUI, wait, b);

        // need test support
        this.snmTestSupport = sharkMessengerApp;
    }

    @Override
    protected void execute() throws Exception {
        try {
            SharkNetMessengerComponent messenger = this.getSharkMessengerApp().getSharkMessengerComponent();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ASAPSerialization.writeCharSequenceParameter(this.getStringArgument(), baos);
            byte[] contentBytes = baos.toByteArray();

            // send message
            messenger.sendSharkMessage(
                    SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE, // label is a string
                    contentBytes, SNMAppSupportingDistributedTesting.TEST_BLOCK_RELEASE_CHANNEL, // specific channel
                    (CharSequence) null, // no specific receiver
                    false, // no signing
                    false // no encryption
            );
            this.getSharkMessengerApp().tellUI("release sent: " + this.getStringArgument());
        } catch (SharkException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }
    }

    @Override
    public String getDescription() {
        return "send release message - blocked SNM peers will be unblocked";
    }
}
