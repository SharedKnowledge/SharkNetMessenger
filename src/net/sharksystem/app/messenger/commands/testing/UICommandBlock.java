package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.ui.messenger.cli.distributedtesting.SNMAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleString;

public class UICommandBlock extends AbstractCommandWithSingleString {
    SNMAppSupportingDistributedTesting snmTestSupport;

    public UICommandBlock(SNMAppSupportingDistributedTesting sharkMessengerApp, SharkNetMessengerUI smUI, String wait, boolean b) {
        super(sharkMessengerApp, smUI, wait, b);

        // need test support
        this.snmTestSupport = sharkMessengerApp;
    }

    @Override
    protected void execute() throws Exception {
        this.snmTestSupport.tellUI("block and wait for release " + this.getStringArgument());
        this.snmTestSupport.block(this.getStringArgument());
        this.snmTestSupport.tellUI("block released " + this.getStringArgument());
    }

    @Override
    public String getDescription() {
        return "blocks execution until release message received";
    }
}
