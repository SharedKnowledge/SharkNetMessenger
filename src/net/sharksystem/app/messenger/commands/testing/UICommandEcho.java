package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleString;

public class UICommandEcho extends AbstractCommandWithSingleString {
    public UICommandEcho(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI smUI, String echo, boolean b) {
        super(sharkMessengerApp, smUI, echo, b);
    }

    @Override
    protected void execute() throws Exception {
        this.getSharkMessengerApp().tellUI(this.getStringArgument());
    }

    @Override
    public String getDescription() {
        return "echo command";
    }
}
