package net.sharksystem.ui.messenger.cli.commands.pki;

import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.pki.PKIHelper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractCommandProduceCertificateListByPeerID extends UICommand {
    private final UICommandStringArgument peer;
    private String peerID = null;

    public AbstractCommandProduceCertificateListByPeerID(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                                         String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        this.peer = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            this.getSharkMessengerApp().tellUI("no peer name provided - use local peer ("
                    + this.getSharkMessengerApp().getPeerName() + ")");
            try {
                this.peerID = this.getSharkMessengerApp().getSharkPeer().getPeerID().toString();
                return true;
            } catch (SharkException e) {
                this.getSharkMessengerApp().tellUIError("fatal: cannot get peer id: " + e);
                return false;
            }
        } else {
            boolean isParsable = this.peer.tryParse(arguments.get(0));
            if (!isParsable) {
                System.err.println("failed to parse issuer id" + arguments.get(0));
            }
            return isParsable;
        }

    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
    }

    protected void ensurePeerIDSet() {

    }

    protected void tellUICertificates(Collection<ASAPCertificate> certificateCollection) {
        if(certificateCollection == null | certificateCollection.isEmpty()) {
            this.getSharkMessengerApp().tellUI("not matching certificates found");
        }
        for (ASAPCertificate certificate : certificateCollection) {
            String sb = PKIHelper.asapCert2String(certificate) +
                    "\n";
            this.getSharkMessengerApp().tellUI(sb);
        }
    }

    @Override
    protected void execute() throws Exception {
        // ensure peer ID is present
        if (this.peerID == null) {
            this.peerID = this.peer.getValue();
        }

        // print results
        Collection<ASAPCertificate> certs = this.produceCertificateCollection(this.peerID);
        if(certs == null || certs.isEmpty()) {
            // asked for yourself?
            if(this.peerID.equalsIgnoreCase(this.getSharkMessengerApp().getPeerName())) {
                this.getSharkMessengerApp().tellUI("it is your peer *name* you used as parameter " +
                        "- look for your certificates");
                certs =
                    this.produceCertificateCollection(this.getSharkMessengerApp().getSharkPeer().getPeerID().toString());

            } else {
                try {
                    Set<PersonValues> persons =
                            this.getSharkMessengerApp().getSharkPKIComponent().getPersonValuesByName(this.peerID);

                    if (persons != null && !persons.isEmpty()) {
                        this.getSharkMessengerApp().tellUI("there is no peer with " + this.peerID
                                + " as id but as *name*, check for credentials");
                        for (PersonValues person : persons) {
                            this.getSharkMessengerApp().tellUI(PKIHelper.personalValue2String(person));
                            this.tellUICertificates(this.produceCertificateCollection(person.getUserID().toString()));
                            this.getSharkMessengerApp().tellUI("-------------------\n");
                        }
                    } else {
                        this.getSharkMessengerApp().tellUI("nothing found for " + this.peerID + " as peer name or id");
                    }
                } catch (ASAPException as) {
                    // okay, there is nothing not under an id or a name
                }
                return;
            }
        }
        this.tellUICertificates(certs);
    }

    protected abstract Collection<ASAPCertificate> produceCertificateCollection(String peerID) throws ASAPSecurityException;
}