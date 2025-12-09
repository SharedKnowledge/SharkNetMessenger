package net.sharksystem.app.messenger.commands.persons;

import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandNoParameter;
import net.sharksystem.pki.PKIHelper;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.app.messenger.commands.pki.PKIUtils;

public class UICommandListPersons extends AbstractCommandNoParameter {
    public UICommandListPersons(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();
        StringBuilder sb = new StringBuilder();

        sb.append("We know ");
        sb.append(pki.getNumberOfPersons());
        sb.append(" persons\n");

        boolean first = true;
        for(int i = 0; i < pki.getNumberOfPersons(); i++) {
            if(first) first = false;
            else sb.append("\n");
            sb.append(i+1);
            sb.append(": ");
            PersonValues personValues = pki.getPersonValuesByPosition(i);
            sb.append(PKIHelper.personalValue2String(personValues));
            int identityAssurance = pki.getIdentityAssurance(personValues.getUserID());
            sb.append(" | iA:  ");
            sb.append(identityAssurance);
            sb.append(" (");
            sb.append(PKIUtils.getIAExplainText(identityAssurance));
            sb.append(")");
        }
        if(!first) {
            sb.append("\n-----------------------------------------------------------------------------------------------------------------");
            sb.append("\nid .. peer id.. chosen be peer itself.");
            sb.append("\nname .. peer name. can be changed");
            sb.append("\nsf..signing failure; what's the chance that a certificate issued by this peer is wrong");
            sb.append("\n\t1 - best setting, hardly fails, 10 certs issued by this person cannot be trusted.");
            sb.append("\niA..identity assurance .. how sure can we be about persons identity (calculated by iA and certificate chain)");
            sb.append("\n\t0 an existing public key cannot be verified, 10 - got that key directly");
            sb.append("\n-----------------------------------------------------------------------------------------------------------------");
        }

        this.getSharkMessengerApp().tellUI(sb.toString());
    }

    @Override
    public String getDescription() {
        return "List known persons.";
    }
}
