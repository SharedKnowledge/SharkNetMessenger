package net.sharksystem.ui.messenger.cli.commands.pki;

import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;

import java.util.Collection;

public class UICommandShowCertificatesByIssuer extends AbstractCommandProduceCertificateListByPeerID {
    public UICommandShowCertificatesByIssuer(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                             String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected Collection<ASAPCertificate> produceCertificateCollection(String peerID) throws ASAPSecurityException {
        return this.getSharkMessengerApp().getSharkPKIComponent().getCertificatesByIssuer(peerID);
    }

    @Override
    public String getDescription() {
        // append hint for how to use
        return "show all certificates issued by a specific peer."
                // append hint for how to use
                ;
    }
}