package net.sharksystem.ui.messenger.cli.commands.basics;

import net.sharksystem.asap.ASAPEncounterManagerAdmin;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Command for terminating the messenger.
 */
public class UICommandStatus extends AbstractCommandNoParameter {
    public UICommandStatus(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                           String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    public void execute() throws Exception {
        StringBuilder sb = new StringBuilder();

        // shark peer information
        sb.append("peer information:\n\tname: ");
        sb.append(this.getSharkMessengerApp().getSharkPeer().getSharkPeerName());
        sb.append("\t| id: ");
        sb.append(this.getSharkMessengerApp().getSharkPeer().getPeerID());

        // app settings
        sb.append("\napp settings:\n\t");
        sb.append(this.getSharkMessengerApp().getSettings());

        // pki information
        sb.append("pki status:\n");
        sb.append("\tpersons: ");
        sb.append(this.getSharkMessengerApp().getSharkPKIComponent().getNumberOfPersons());
        sb.append(" | certificates: ");
        sb.append(this.getSharkMessengerApp().getSharkPKIComponent().getCertificates().size());
        sb.append(" | keys created: ");
        sb.append(DateTimeHelper.long2DateString(
                this.getSharkMessengerApp().getSharkPKIComponent().getKeysCreationTime()));

        sb.append("\n\tpublic key fingerprint:\t");
        sb.append(ASAPCryptoAlgorithms.getFingerprint(
                this.getSharkMessengerApp().getSharkPKIComponent().getASAPKeyStore().getPublicKey()));

        sb.append("\nhub connections:\n");
        sb.append("\thubs connected: ");
        sb.append(this.getSharkMessengerApp().getHubConnectionManager().getConnectedHubs().size());
        sb.append(" | failed to connect: ");
        sb.append(this.getSharkMessengerApp().getHubConnectionManager().getFailedConnectionAttempts().size());

        ASAPEncounterManagerAdmin encounterManagerAdmin = this.getSharkMessengerApp().getEncounterManagerAdmin();
        sb.append("\nencounter status:");
        /*
        sb.append("\n\tcool down periode in ms: ");
        sb.append(encounterManagerAdmin.getTimeBeforeReconnect());
         */
        /*
        sb.append("\n\tsum encountered peers: ");
        int numberEncounter = this.getSharkMessengerApp().getEncounterLogs().size();
        sb.append(numberEncounter);
        if(numberEncounter > 0) {
            sb.append("\n");
            Map<CharSequence, Date> encounterTime = encounterManagerAdmin.getEncounterTime();
            Set<CharSequence> connectedPeerIDs = encounterManagerAdmin.getConnectedPeerIDs();
            int index = 1;
            for(CharSequence peerID : encounterTime.keySet()) {
                if(index != 1) sb.append("\n");
                sb.append("\t#");
                sb.append(index++);
                sb.append(": ");
                sb.append(peerID);
                sb.append(" | ");
                sb.append(DateTimeHelper.long2ExactTimeString(encounterTime.get(peerID).getTime()));
                if(connectedPeerIDs.contains(peerID)) {
                    sb.append(" | connected");
                }
            }
        }
         */

        this.getSharkMessengerApp().tellUI(sb.toString());
    }

    @Override
    public String getDescription() {
        return "Show app setting - todo: should be changeable.";
    }
}
