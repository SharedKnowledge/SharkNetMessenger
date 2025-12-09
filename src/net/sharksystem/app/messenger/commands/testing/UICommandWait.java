package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleInteger;

public class UICommandWait extends AbstractCommandWithSingleInteger {
    public UICommandWait(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI smUI, String wait, boolean b) {
        super(sharkMessengerApp, smUI, wait, b);
    }

    @Override
    protected void execute() {
        this.getSharkMessengerApp().tellUI("wait " + this.getIntegerArgument() + " ms ...");

        try {
            Thread.sleep(this.getIntegerArgument());
        } catch (InterruptedException e) {
            this.getSharkMessengerApp().tellUIError("wait received interrupt - that shouldn't happen");
            e.printStackTrace();
        }
        this.getSharkMessengerApp().tellUI("resume");
    }

    @Override
    public String getDescription() {
        return "waits a milliseconds";
    }
}
