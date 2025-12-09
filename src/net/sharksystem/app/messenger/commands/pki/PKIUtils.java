package net.sharksystem.app.messenger.commands.pki;

import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.asap.ASAPEncounterConnectionType;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class PKIUtils {
    private final SharkPKIComponent pki;

    public PKIUtils(SharkPKIComponent pki) {
        this.pki = pki;
    }

    public String getIAString(CharSequence peerID) {
        StringBuilder sb = new StringBuilder();
        sb.append("ia (");
        sb.append(peerID);
        sb.append("): ");

        int ia = 0;
        try {
            ia = pki.getIdentityAssurance(peerID);
        } catch (ASAPSecurityException e) {
            // nothing found
            // is it you
            if(pki.getOwnerID().equals(peerID)) {
                ia = 10;
            }
        }
        sb.append(ia);
        sb.append(" (");
        sb.append(PKIUtils.getIAExplainText(ia));
        sb.append(") ");

        return sb.toString();
    }

    public static String getIAExplainText(int ia) {
        if(ia < 0 || ia > 10) return "error: iA must be in [0,10]";
        if(ia == 0) return("bad");
        else if(ia == 10) return("perfect");
        else if(ia == 9) return("good");
        else if(ia > 6) return("nice");
        else if(ia < 4) return("bad");

        return("enough?");
    }

    public String getCertificateAsString(ASAPCertificate cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("issued by: ");
        sb.append(" name: ");
        sb.append(cert.getIssuerName());
        sb.append(" | id: ");
        sb.append(cert.getIssuerID());
        sb.append("\n");

        sb.append("for subject: ");
        sb.append(" name: ");
        sb.append(cert.getSubjectName());
        sb.append(" | id: ");
        sb.append(cert.getSubjectID());
        sb.append("\n");

        sb.append("valid since: ");
        sb.append(DateTimeHelper.long2DateString(cert.getValidSince().getTimeInMillis()));
        sb.append(" | valid until: ");
        sb.append(DateTimeHelper.long2DateString(cert.getValidUntil().getTimeInMillis()));
        sb.append("\n");

        sb.append("public key finger print: ");
        try {
            sb.append(ASAPCryptoAlgorithms.getFingerprint(cert.getPublicKey()));
        } catch (NoSuchAlgorithmException e) {
            sb.append(e.getLocalizedMessage());
        }

        sb.append("\ncredentials received via ");
        ASAPEncounterConnectionType connectionType = cert.getConnectionTypeCredentialsReceived();
        sb.append(connectionType);
        sb.append(" | ");
        if(connectionType != ASAPEncounterConnectionType.AD_HOC_LAYER_2_NETWORK) {
            sb.append("hope identity was checked carefully.");
        } else {
            sb.append("direct encounter - well done");

        }
        return sb.toString();
    }

    public static PersonValues getUniquePersonValues(String peerID, SharkNetMessengerApp app)
            throws SharkNetMessengerException, ASAPException {
        Set<PersonValues> personValuesByName = app.getSharkPKIComponent().getPersonValuesByName(peerID);
        if(personValuesByName.size() > 1) {
            throw new SharkNetMessengerException("problem: more than one persons found with name " + peerID);
        }
        return personValuesByName.iterator().next();
    }
}
