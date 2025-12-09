package net.sharksystem.app.messenger.commands;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.*;
import net.sharksystem.app.messenger.commands.messenger.ChannelPrinter;
import net.sharksystem.app.messenger.commands.messenger.SNMessagesSerializer;
import net.sharksystem.app.messenger.commands.pki.PKIUtils;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.utils.SerializationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SNM_JSON_Builder {
    public static final String COMMAND_KEY = "command";
    public static final String STATUS_KEY = "status";
    public static final String STATUS_VALUE_SUCCESS = "success";
    public static final String STATUS_VALUE_ERROR = "error";

    public static final String STATUS_ERROR_MESSAGE_KEY = "error";
    private static final String PAYLOAD_KEY = "payload";
    private static final String NAME_KEY = "name";
    private static final String URI_KEY = "uri";
    private static final String MESSAGE_COUNT_KEY = "messageCount";
    private static final String AGE_KEY = "age";
    private static final String MESSAGES_KEY = "messages";
    private static final String INDEX_KEY = "index";
    private static final String CONTENT_TYPE_KEY = "contentType";
    private static final String CONTENT_KEY = "content";
    private static final String SENDER_ID_KEY = "senderID";
    private static final String SENDER_NAME_KEY = "senderName";
    private static final String RECEIVER_KEY = "recipients";
    private static final String TIME_KEY = "time";
    private static final String ENCRYPTED_KEY = "encrypted";
    private static final String SIGNED_KEY = "signed";
    private static final String VERIFIED_KEY = "verified";
    private static final String IDENTITY_ASSURANCE_KEY = "identityAssurance";
    private static final String HOPING_LIST_KEY = "hopingList";

    private boolean payloadOpened = false;
    private boolean firstKey = true;

    StringBuilder sb = new StringBuilder();


    public SNM_JSON_Builder(String command) {
        this.openSubObject();
        this.appendKey(COMMAND_KEY);
        this.appendValue(command);

    }

    /// ////////////////////////////////////////// basic support ////////////////////////////////////////////////////
    private void appendKey(String key) {
        if(this.firstKey) this.firstKey = false;
        else this.sb.append(",\n");

        this.sb.append("\"");
        this.sb.append(key);
        this.sb.append("\": ");
    }

    private int objectDepth = 0;
    private void openSubObject() {
        this.sb.append("{\n");
        this.firstKey = true;
        this.objectDepth++;
    }

    private int closeSubObject() {
        if(this.objectDepth < 1) System.err.println("closing object on level 0 - that's a bug in your code");
        this.sb.append("}");
        this.firstKey = false;
        this.objectDepth--;
        return this.objectDepth;
    }

    private void appendValue(CharSequence value) {
        this.appendValue(value.toString());
    }

    private void appendValue(int value) {
        this.appendValue(String.valueOf(value));
    }

    private void appendValue(String value) {
        this.sb.append("\"");
        this.sb.append(value);
        this.sb.append("\"");
    }

    private void appendValue(boolean value) {
        if(value) this.appendValue("true");
        else this.appendValue("false");
    }

    public String getJsonString() {
        if(this.objectDepth > 0) {
            while(this.closeSubObject() > 0);
        }
        return sb.toString();
    }

    public void error(String errorMessage) {
        this.appendKey(STATUS_KEY);
        this.appendValue(STATUS_VALUE_ERROR);
        this.appendKey(STATUS_ERROR_MESSAGE_KEY);
        this.appendValue(errorMessage);
    }

    private void openList() {
        this.sb.append("[ ");
    }

    private void closeList() {
        this.sb.append("] ");
    }

    /// ////////////////////////////////////////// channel ////////////////////////////////////////////////////
    public void addChannel(SharkNetMessengerChannel channel) throws IOException, SharkNetMessengerException {
        this.appendKey(PAYLOAD_KEY);
        this.openSubObject();

        this.appendKey(NAME_KEY);
        this.appendValue(channel.getName());
        this.appendKey(URI_KEY);
        this.appendValue(channel.getURI());
        this.appendKey(MESSAGE_COUNT_KEY);
        this.appendValue(Integer.toString(channel.getMessages().size()));
        this.appendKey(AGE_KEY);
        SharkNetCommunicationAge age = channel.getAge();
        switch (age) {
            case BRONZE_AGE:
                this.appendValue("bronze");
                break;
            case STONE_AGE:
                this.appendValue("stone");
                break;
            case NETWORK_AGE:
                this.appendValue("network");
                break;
            default:
                this.appendValue("unknown");
                break;
        }
    }

    public void addMessages(SharkNetMessengerApp snmApp, SharkPKIComponent pki, SharkNetMessageList messages)
            throws IOException, SharkNetMessengerException, ASAPException {

        this.appendKey(MESSAGES_KEY);
        this.openList();
        for (int i = 0; i < messages.size(); i++) {
            this.openSubObject();
            this.appendKey(INDEX_KEY);
            this.appendValue(i + 1);

            SharkNetMessage message = messages.getSharkMessage(i, true);
            this.addMessageDetails(snmApp, pki, message);

            this.closeSubObject();
            if(i < messages.size() - 1 ) this.sb.append(", ");
        }
        this.closeList();
    }

    private void addMessageDetails(SharkNetMessengerApp snmApp, SharkPKIComponent pki, SharkNetMessage message)
            throws IOException, ASAPException {

        // content type
        this.appendKey(CONTENT_TYPE_KEY);
        CharSequence contentType = message.getContentType();
        this.appendValue(message.getContentType());

        // content
        this.appendKey(CONTENT_KEY);
        byte[] content = message.getContent();
        if(content.length< 1) {
            this.appendValue("no content");
        }

        else if(contentType.toString().equalsIgnoreCase(SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE)) {
            this.appendValue(SerializationHelper.bytes2characterSequence(content));
        }
        else if(contentType.toString().equalsIgnoreCase(SharkNetMessage.SN_CONTENT_TYPE_FILE)) {
            StringBuilder sb = new StringBuilder();

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
                    if (localFile.exists()) {
                        sb.append(" | already saved");
                    } else {
                        FileOutputStream fos = new FileOutputStream(localFile);
                        fos.write(snFileMessage.getFileContent());
                        fos.close();
                        sb.append(" | saved to disc");
                    }
                } catch (IOException ie) {
                    sb.append(" | cannot access file: " + ie.getLocalizedMessage());
                }
                sb.append(" - ");
            } catch (SharkException e) {
                sb.append("problems: " + e.getLocalizedMessage());
                sb.append(" - ");
            }

            this.appendValue(sb.toString());
        }
        else {
            StringBuilder sb = new StringBuilder();
            // unknown format
            sb.append("msg: \"");
            sb.append(snmApp.produceStringForMessage(contentType, content));
            sb.append("\", len: ");
            sb.append(contentType.length());
            sb.append(" byte(s): ");
            sb.append(" - ");
            this.appendValue(sb.toString());
        }

        // sender
        this.appendKey(SENDER_ID_KEY);
        CharSequence senderID = message.getSender();

        boolean look4sender = true;
        if(pki.getOwnerID().toString().equalsIgnoreCase(senderID.toString())) {
            look4sender = false;
            this.appendValue("you");
        } else {
            this.appendValue(senderID);
        }

        this.appendKey(SENDER_NAME_KEY);
        if(!look4sender) {
            this.appendValue("you");
        } else {
            try {
                PersonValues personValuesByID = pki.getPersonValuesByID(senderID);
                if (personValuesByID != null) {
                    CharSequence senderName = personValuesByID.getName();
                    this.appendValue(senderName);
                }
            } catch (SharkException se) {
                this.appendValue("unknown");
            }
        }

        // recipients
        this.appendKey(RECEIVER_KEY);
        Set<CharSequence> recipients = message.getRecipients();
        if(recipients.isEmpty()) {
            this.appendValue("not specified");
        } else {
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (CharSequence recipient : recipients) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(recipient.toString());
            }
            sb.append("]");
            this.appendValue(sb.toString());
        }

        // time
        this.appendKey(TIME_KEY);
        this.appendValue(DateTimeHelper.long2ExactTimeString(message.getCreationTime()));

        // encryption / verification
        this.appendKey(ENCRYPTED_KEY);
        this.appendValue(message.encrypted());

        /*
        if(message.encrypted())
        {
            sb.append(" | can decrypt: ");
            sb.append(this.returnYesNo(message.couldBeDecrypted()));
        }
         */

        // signed
        this.appendKey(SIGNED_KEY);
        this.appendValue(message.signed());

        // verified
        this.appendKey(VERIFIED_KEY);
        this.appendValue(message.verified());

        // ia
        this.appendKey(IDENTITY_ASSURANCE_KEY);
        this.appendValue(new PKIUtils(pki).getIAString(message.getSender()));

        // hoping list
        this.appendKey(HOPING_LIST_KEY);
        StringBuilder sb = new StringBuilder();
        sb.append("hoping list: ");
        List<ASAPHop> asapHopsList = message.getASAPHopsList();
        if(asapHopsList.isEmpty()) {
            sb.append("no hops");
        } else {
            ChannelPrinter cp = new ChannelPrinter();
            int i = 0;
            sb.append(">>");
            for (ASAPHop hop : asapHopsList) {
                sb.append(i++);
                sb.append(": ");
                cp.addHobDetails(sb, hop);
            }
            sb.append("<<");
        }
        this.appendValue(sb.toString());
    }
}
