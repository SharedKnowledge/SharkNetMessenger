package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessage;
import net.sharksystem.app.messenger.SharkNetMessengerChannel;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.app.messenger.commands.commandarguments.*;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.messenger.SNMessagesSerializer;
import net.sharksystem.app.messenger.commands.messenger.UICommandProduceChannelListBefore;
import net.sharksystem.app.messenger.commands.pki.PKIUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This command sends a message into a channel.
 */
public class UICommandExistsMessage extends UICommandProduceChannelListBefore {
    private final UICommandStringArgument contentArgument;
    private final UICommandStringArgument contentTypeArgument;
    private final UICommandBooleanArgument signArgument;
    private final UICommandBooleanArgument encryptArgument;
    private final UICommandIntegerArgument channelIndexArgument;
    private final UICommandStringArgument receiverArgument;

    private String content;
    private String contentType;
    private boolean sign;
    private String receiverName;
    private boolean encrypt;
    private int channelIndex;

    public UICommandExistsMessage(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                  String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.contentArgument = new UICommandStringArgument(sharkMessengerApp);
        this.contentTypeArgument = new UICommandStringArgument(sharkMessengerApp);
        this.signArgument = new UICommandBooleanArgument(sharkMessengerApp);
        this.receiverArgument = new UICommandStringArgument(sharkMessengerApp);
        this.encryptArgument = new UICommandBooleanArgument(sharkMessengerApp);
        this.channelIndexArgument = new UICommandIntegerArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("Index target channel (0..n): ", this.channelIndexArgument)
                .addQuestion("Sign? ", this.signArgument)
                //.addQuestion("Encrypt? ", this.encrypt)
                .addQuestion("Message: ", this.contentArgument)
                //.addQuestion("Receivers (leave blank for anybody): ", this.receivers)
                .build();
    }

    @Override
    public void execute() throws Exception {
        try {
            SharkNetMessengerComponent messenger = this.getSharkMessengerApp().getSharkMessengerComponent();
            int index = this.channelIndex - 1;
            SharkNetMessengerChannel channel;
            CharSequence channelURI;
            try {
                channelURI = messenger.getChannel(index).getURI();
            }
            catch(SharkException se) {
                if(this.channelIndex == 1) {
                    // okay, go with default
                    channelURI = SharkNetMessengerComponent.GENERAL_CHANNEL_URI;
                } else {
                    this.getSharkMessengerApp().tellUIError("create a channel on index " + this.channelIndex + "before.");
                    return;
                }
            }

            CharSequence receiverID = this.receiverName;
            if(!this.receiverName.equalsIgnoreCase(SharkNetMessage.ANY_SHARKNET_PEER)) {
                // find id for name
                try {
                    receiverID =
                            PKIUtils.getUniquePersonValues(this.receiverName, this.getSharkMessengerApp()).getUserID();
                }
                catch(ASAPException ae) {
                    this.getSharkMessengerApp().tellUIError("do not know receiver peer (yet): " + this.receiverName);
                    return;
                }
            }

            // produce content bytes
            byte[] contentBytes = null;
            String effectiveFormat = SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE;
            switch(this.contentType) {
                default:
                    String sb = "do not know content type: " +
                            this.contentType +
                            " - but convert into bytes with ASAPSerialization";
                    this.getSharkMessengerApp().tellUI(sb);

                case SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE:
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ASAPSerialization.writeCharSequenceParameter(this.content, baos);
                    contentBytes = baos.toByteArray();
                    break;

                case SharkNetMessage.SN_CONTENT_TYPE_FILE:
                    effectiveFormat = SharkNetMessage.SN_CONTENT_TYPE_FILE;
                    baos = new ByteArrayOutputStream();
                    SNMessagesSerializer.serializeFile(this.content, baos);
                    contentBytes = baos.toByteArray();
                    break;

            }
            // send message
            messenger.sendSharkMessage(effectiveFormat,
                    contentBytes, channelURI, receiverID, this.sign, this.encrypt);
        } catch (SharkException | IOException e) {
            this.getSharkMessengerApp().tellUIError(e.getLocalizedMessage());
        }
    }

    private Set<CharSequence> getAllExistingPeers(String s) {
        Set<CharSequence> peers = new HashSet<>();
        String[] stringPeers = s.split(",");
        Collections.addAll(peers, stringPeers);
        return peers.size() > 0 ? peers : null;
    }

    @Override
    public String getDescription() {

        String sb = "does message exist: content(string) [content, contentType (" +
                SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE +
                ")" +

                // channel
                ", channel index (1)" +

                // signing
                ", sign (true)" +

                // encryption
                ", receiverName (" +
                SharkNetMessage.ANY_SHARKNET_PEER +
                ")" +
                ", encrypt (false)" +

                "]";
        return sb;
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        // set defaults:
        this.contentType = SharkNetMessage.SN_CONTENT_TYPE_ASAP_CHARACTER_SEQUENCE;
        this.sign = true;
        this.receiverName = SharkNetMessage.ANY_SHARKNET_PEER;
        this.encrypt = false;
        this.channelIndex = 1;

        // mandatory arguments
        if(arguments.size() < 1) {
            this.getSharkMessengerApp().tellUIError("content (a string) is mandatory");
            return false;
        }
        if(!contentArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse message content: " + arguments.get(0));
            return false;
        } else {
            this.content = this.contentArgument.getValue();
        }

        // optional parameters
        // content type
        if(arguments.size() > 1) {
            if (!contentTypeArgument.tryParse(arguments.get(1))) {
                this.getSharkMessengerApp().tellUIError("cannot parse content type: " + arguments.get(1));
                return false;
            } else {
                this.contentType = this.contentTypeArgument.getValue();
            }
        }

        // channel index
        if(arguments.size() > 2) {
            if(!this.channelIndexArgument.tryParse(arguments.get(2))) {
                this.getSharkMessengerApp().tellUIError("cannot parse channel index: " + arguments.get(2));
                return false;
            } else {
                this.channelIndex = this.channelIndexArgument.getValue();
            }
        }

        // sign
        if(arguments.size() > 3) {
            if(!this.signArgument.tryParse(arguments.get(3))) {
                this.getSharkMessengerApp().tellUIError("cannot parse sign(true/false): " + arguments.get(3));
                return false;
            } else {
                this.sign = this.signArgument.getValue();
            }
        }

        // receiver
        if(arguments.size() > 4) {
            if (!this.receiverArgument.tryParse(arguments.get(4))) {
                this.getSharkMessengerApp().tellUIError("cannot parse receiver peer name: " + arguments.get(4));
                return false;
            } else {
                this.receiverName = this.receiverArgument.getValue();
            }
        }

        // encrypt
        if(arguments.size() > 4) {
            if(!this.encryptArgument.tryParse(arguments.get(5))) {
                this.getSharkMessengerApp().tellUIError("cannot parse encrypt(true/false): " + arguments.get(5));
                return false;
            } else {
                this.encrypt = this.encryptArgument.getValue();
            }
        }

        return true;
    }
}
