package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleInteger;

/**
 * Command for terminating the messenger.
 */
public class UICommandTimeBomb extends AbstractCommandWithSingleInteger {
    public UICommandTimeBomb(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                             String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    public void execute() throws Exception {
        int millis = this.getIntegerArgument();
        this.getSharkMessengerApp().tellUI("going to kill this process in " + millis +" ms");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(UICommandTimeBomb.this.getIntegerArgument());
                    UICommandTimeBomb.this.getSharkMessengerApp().tellUI("max time ended: end CLI for peer " +
                            UICommandTimeBomb.this.getSharkMessengerApp().getSharkPeer().getPeerID());
                } catch (SharkException | InterruptedException e) {
                    UICommandTimeBomb.this.getSharkMessengerApp().tellUI("max time ended: end CLI");
                }
                System.exit(0);
            }
        }).start();
    }

    @Override
    public String getDescription() {
        return "Terminates the messenger after some milliseconds";
    }
}
