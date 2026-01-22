package net.sharksystem.ui.messenger.cli.commands.pki;

import java.util.Collection;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

public class UICommandShowCertificatesBySubject extends AbstractCommandProduceCertificateListByPeerID {
    public UICommandShowCertificatesBySubject(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                              String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected Collection<ASAPCertificate> produceCertificateCollection(String peerID) throws ASAPSecurityException {
        return this.getSharkMessengerApp().getSharkPKIComponent().getCertificatesBySubject(peerID);
    }

    @Override
    public String getDescription() {
        // append hint for how to use
        return "show all certificates issued for a specific peer (the subject)."
                // append hint for how to use
                ;
    }
}