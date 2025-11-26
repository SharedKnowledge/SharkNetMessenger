package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleString;

public class UICommandBlock extends AbstractCommandWithSingleString {
    public UICommandBlock(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI smUI, String wait, boolean b) {
        super(sharkMessengerApp, smUI, wait, b);
    }

    @Override
    protected void execute() throws Exception {
        this.getSharkMessengerApp().tellUI("block and wait for release " + this.getStringArgument());
        this.getSharkMessengerApp().block(this.getStringArgument());
        this.getSharkMessengerApp().tellUI("block released " + this.getStringArgument());
    }

    @Override
    public String getDescription() {
        return "blocks execution until release message received";
    }
}
