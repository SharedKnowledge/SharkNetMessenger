package net.sharksystem.ui.messenger.cli.commands.pki;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;
import net.sharksystem.pki.CredentialMessage;
import net.sharksystem.pki.PKIHelper;

import java.util.List;

public class UICommandShowPendingCredentials extends AbstractCommandNoParameter {
    public UICommandShowPendingCredentials(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                           String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        List<CredentialMessage> pendingCredentialMessages = this.getSharkMessengerApp().getPendingCredentialMessages();

        if(pendingCredentialMessages == null || pendingCredentialMessages.isEmpty()) {
            this.getSharkMessengerApp().tellUI("no pending credential messages");
            return;
        }

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for(CredentialMessage credentialMessage : pendingCredentialMessages) {
            sb.append("# ");
            sb.append(i++);
            sb.append("\n-------------\n");
            sb.append(PKIHelper.credentialMessage2String(credentialMessage));
            sb.append("\n-------------\n\n");
        }

        this.getSharkMessengerApp().tellUI(sb.toString());
    }

    @Override
    public String getDescription() {
        return "show pending (not yet refused or accepted) credentials";
    }
}