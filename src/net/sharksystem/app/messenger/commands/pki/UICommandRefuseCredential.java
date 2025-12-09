package net.sharksystem.app.messenger.commands.pki;

import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleInteger;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

public class UICommandRefuseCredential extends AbstractCommandWithSingleInteger {
    public UICommandRefuseCredential(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                     String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        this.getSharkMessengerApp().refusePendingCredentialMessageOnIndex(this.getIntegerArgument());
    }

    @Override
    public String getDescription() {
        return "refuse pending credential message";
    }
}