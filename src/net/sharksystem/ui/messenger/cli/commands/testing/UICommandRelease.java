package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleInteger;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UICommandRelease extends AbstractCommandWithSingleString {
    public UICommandRelease(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI smUI, String wait, boolean b) {
        super(sharkMessengerApp, smUI, wait, b);
    }

    @Override
    protected void execute() throws Exception {
        try {
            SharkNetMessengerComponent messenger = this.getSharkMessengerApp().getSharkMessengerComponent();
            byte[] contentBytes = null;

            String effectiveFormat = SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ASAPSerialization.writeCharSequenceParameter(this.getStringArgument(), baos);
            contentBytes = baos.toByteArray();

            // send message
            messenger.sendSharkMessage(effectiveFormat,
                    contentBytes, SharkNetMessengerApp.TEST_BLOCK_RELEASE_CHANNEL, (CharSequence)null,
                    false, false);
            this.getSharkMessengerApp().tellUI("release block send: " + this.getStringArgument());
        } catch (SharkException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }    }

    @Override
    public String getDescription() {
        return "send release message - blocked SNM peers will be unblocked";
    }
}
