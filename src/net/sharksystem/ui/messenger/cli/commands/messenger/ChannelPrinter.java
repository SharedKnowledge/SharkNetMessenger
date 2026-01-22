package net.sharksystem.ui.messenger.cli.commands.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.*;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.ui.messenger.cli.commands.pki.PKIUtils;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.SerializationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ChannelPrinter {
    public static String getChannelDescription(SharkNetMessengerChannel channel)
            throws IOException, SharkNetMessengerException {

        StringBuilder sb = new StringBuilder();

        sb.append("name: ");
        sb.append(channel.getName());
        sb.append(" | uri: ");
        sb.append(channel.getURI());
        sb.append(" | #messages: ");
        sb.append(channel.getMessages().size());
        sb.append(" | age: ");
        SharkNetCommunicationAge age = channel.getAge();
        switch (age) {
            case BRONZE_AGE: sb.append("bronze"); break;
            case STONE_AGE: sb.append("stone"); break;
            case NETWORK_AGE: sb.append("network"); break;
            default: sb.append("unknown"); break;
        }

        return sb.toString();
    }

    public static String getChannelDescriptions(SharkNetMessengerComponent messengerComponent)
                throws IOException, SharkNetMessengerException {

        StringBuilder sb = new StringBuilder();

        List<CharSequence> channelUris = messengerComponent.getChannelUris();
        if(channelUris.isEmpty()) {
            sb.append("no channels\n");
        } else {
            sb.append(channelUris.size());
            if(channelUris.size() > 1) sb.append(" channels:\n");
            else sb.append(" channel:\n");

            int i = 1;
            for (CharSequence channelUri : channelUris) {
                sb.append(i++ + ": ");
                SharkNetMessengerChannel channel = messengerComponent.getChannel(channelUri);
                sb.append(getChannelDescription(channel));
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String returnYesNo(boolean value) {
        if(value) return "yes";
        else return "no";
    }

    public String getMessagesASString(SharkPKIComponent pki, String channelUri, SharkNetMessageList messages)
            throws IOException, SharkNetMessengerException, ASAPException {

        StringBuilder sb = new StringBuilder();

        sb.append("list messages in channel: " + channelUri);
        if(messages.size() < 1) {
            sb.append(": no messages\n");
            return sb.toString();
        }
        else {
            sb.append("\n");
            for (int i = 0; i < messages.size(); i++) {
                sb.append("#");
                sb.append(i+1);
                sb.append(" ---------------------------------------------------------------------------------\n");
                SharkNetMessage message = messages.getSharkMessage(i, true);
                sb.append(this.getMessageDetails(pki, message));
                sb.append("\n/#");
                sb.append(i+1);
                sb.append(" --------------------------------------------------------------------------------\n");
            }
        }
        return sb.toString();
    }

    public String getMessageDetails(SharkPKIComponent pki, SharkNetMessage message)
            throws IOException, ASAPException {
        StringBuilder sb = new StringBuilder();

        CharSequence contentType = null;
        try {
            // content type
            contentType = message.getContentType();
            sb.append("content type: ");
            sb.append(contentType);
            sb.append(" | ");
        }
        catch(ASAPSecurityException ase) {
            // happens if message cannot be decrypted we are not receiver.
            return "message cannot be decrypted - we are not receiver or lost private key";
        }

        // content
        byte[] content = message.getContent();
        if (content.length < 1) {
            sb.append("no content\n");
        } else if(contentType.toString().equalsIgnoreCase(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE)) {
            sb.append("'");
            sb.append(SerializationHelper.bytes2characterSequence(content));
            sb.append("'\n");
        } else if(contentType.toString().equalsIgnoreCase(SharkNetMessage.SN_CONTENT_TYPE_FILE)) {
            try {
                SNMessagesSerializer.SNFileMessage snFileMessage =
                        SNMessagesSerializer.deserializeFile(message.getContent());
                sb.append("file: ");
                sb.append(snFileMessage.getFileName());
                sb.append(" (");
                sb.append(snFileMessage.getSize());
                sb.append(" bytes)");
                try {
                    File localFile = new File(snFileMessage.getFileName());
                    if(localFile.exists()) {
                        sb.append(" | already saved");
                    } else {
                        FileOutputStream fos = new FileOutputStream(localFile);
                        fos.write(snFileMessage.getFileContent());
                        fos.close();
                        sb.append(" | saved to disc");
                    }
                }
                catch(IOException ie) {
                    sb.append(" | cannot access file: " + ie.getLocalizedMessage());
                }
                sb.append("\n");
            } catch (SharkException e) {
                sb.append("problems: " + e.getLocalizedMessage());
                sb.append("\n");
            }
        } else {
            sb.append("no parser for this type, cannot parse those byte: ");
            sb.append(contentType.length());
            sb.append("\n");
        }

        // sender
        sb.append("sender: ");
        CharSequence senderID = message.getSender();
        boolean look4sender = true;
        if(pki.getOwnerID().toString().equalsIgnoreCase(senderID.toString())) {
            look4sender = false;
            sb.append("you");
        } else {
            sb.append(senderID);
        }

        if(look4sender) {
            try {
                PersonValues personValuesByID = pki.getPersonValuesByID(senderID);
                if (personValuesByID != null) {
                    CharSequence senderName = personValuesByID.getName();
                    sb.append(" (");
                    sb.append(senderName);
                    sb.append(")");
                }
            } catch (SharkException se) {
                // ignore
            }
        }

        // recipients
        sb.append(" | recipients: ");
        Set<CharSequence> recipients = message.getRecipients();
        if (recipients.size() < 1) sb.append("not specified");
        boolean first = true;
        for (CharSequence recipient : recipients) {
            if (first) first = false;
            else sb.append(";");
            sb.append(recipient.toString());
        }

        // time
        sb.append(" | time: ");
        sb.append(DateTimeHelper.long2ExactTimeString(message.getCreationTime()));

        // encryption / verification
        sb.append("\n");
        sb.append("E2E security: encrypted: ");
        sb.append(this.returnYesNo(message.encrypted()));

        if (message.encrypted()) {
            sb.append(" | can decrypt: ");
            sb.append(this.returnYesNo(message.couldBeDecrypted()));
        }

        sb.append(" | ");
        sb.append("signed: ");
        sb.append(this.returnYesNo(message.signed()));
        if (message.signed()) {
            sb.append(" | verified: ");
            sb.append(this.returnYesNo(message.verified()));
            if(message.verified()) {
                sb.append(" | ");
                sb.append(new PKIUtils(pki).getIAString(message.getSender()));
            }
        }

        // hoping list
        sb.append("\n");
        sb.append("hoping list: ");
        List<ASAPHop> asapHopsList = message.getASAPHopsList();
        if(asapHopsList.isEmpty()) {
            sb.append("no hops");
        } else {
            int i = 0;
            sb.append("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
            for(ASAPHop hop : asapHopsList) {
                sb.append(i++);
                sb.append(": ");
                this.addHobDetails(sb, hop);
                sb.append("\n");
            }
            sb.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
        }

        //sb.append("\n");

        return sb.toString();
    }

    private void addHobDetails(StringBuilder sb, ASAPHop hop) {
        sb.append("sender: ");
        sb.append(hop.sender());
        sb.append(" | ");
        sb.append("P2P security: encrypted: ");
        sb.append(this.returnYesNo(hop.encrypted()));
        sb.append(" | ");
        sb.append("verified: ");
        sb.append(this.returnYesNo(hop.verified()));
        sb.append(" | ");
        sb.append("via: ");
        switch(hop.getConnectionType()) {
            case INTERNET -> sb.append("TCP");
            case ASAP_HUB -> sb.append("HUB");
            case AD_HOC_LAYER_2_NETWORK -> sb.append("Ad-Hoc");
            case ONION_NETWORK -> sb.append("Onion");
        }
    }
}
