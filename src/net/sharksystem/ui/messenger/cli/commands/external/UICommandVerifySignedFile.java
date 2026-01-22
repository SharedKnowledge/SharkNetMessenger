package net.sharksystem.ui.messenger.cli.commands.external;

import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;
import net.sharksystem.ui.messenger.cli.commands.pki.PKIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class UICommandVerifySignedFile extends UICommand {
    private final UICommandStringArgument inputFileNameArgument;
    private final UICommandStringArgument inputSignatureFileNameArgument;
    private final UICommandStringArgument senderPeerNameArgument;
    private String inputFileName;
    private String inputSignatureFileName;
    private String senderPeerName;

    public UICommandVerifySignedFile(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                     String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.inputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.inputSignatureFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.senderPeerNameArgument = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("file name with signature: ", this.inputFileNameArgument)
                .addQuestion("signature file name: ", this.inputSignatureFileNameArgument)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();

        File signedDataFile = new File(this.inputFileName);
        File signatureDataFile = new File(this.inputSignatureFileName);
        FileInputStream signedDataIS = new FileInputStream(signedDataFile);
        FileInputStream signatureIS = new FileInputStream(signatureDataFile);

        long signedDataFileLength = signedDataFile.length();
        if(signedDataFileLength > Integer.MAX_VALUE) {
            this.getSharkMessengerApp().tellUIError("problem: signed file to long - more than MAX_INTEGER byte");
            return;
        }


        byte[] signedData = new byte[(int) signedDataFileLength];
        byte[] signature = new byte[(int) signatureDataFile.length()]; // length most probably 256
        signedDataIS.read(signedData);
        signatureIS.read(signature);

        String senderID = null;
        if(this.getSharkMessengerApp().getPeerName().equalsIgnoreCase(this.senderPeerName)) {
            senderID = this.getSharkMessengerApp().getSharkPeer().getPeerID().toString();
            this.senderPeerName = "you";
        } else {
            senderID = PKIUtils.getUniquePersonValues(this.senderPeerName, this.getSharkMessengerApp())
                    .getUserID().toString();
        }

        if(ASAPCryptoAlgorithms.verify(signedData, signature, senderID, pki.getASAPKeyStore())) {
            StringBuilder sb = new StringBuilder();
            sb.append("verified: file ");
            sb.append(this.inputFileName);
            sb.append(" was signed by ");
            sb.append(this.senderPeerName);
            sb.append(" (ia == ");
            int iA = pki.getIdentityAssurance(senderID);
            sb.append(iA);
            sb.append(" (");
            sb.append(PKIUtils.getIAExplainText(iA));
            sb.append(")) | content was not changed.");
            this.getSharkMessengerApp().tellUI(sb.toString());
        } else {
            this.getSharkMessengerApp().tellUI("cannot verify signature: wrong [key | peer | signature] or content was changed");
        }
    }

    @Override
    public String getDescription() {
        return "decrypt an extern file decrypted for this peer.";
    }
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 3) {
            this.getSharkMessengerApp().tellUIError("required: signed file name, signature file name, sender name");
            return false;
        }

        if(!this.inputFileNameArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse signed file name: " + arguments.get(0));
            return false;
        } else {
            this.inputFileName = this.inputFileNameArgument.getValue();
        }

        if(!this.inputSignatureFileNameArgument.tryParse(arguments.get(1))) {
            this.getSharkMessengerApp().tellUIError("cannot parse signature file name: " + arguments.get(1));
            return false;
        } else {
            this.inputSignatureFileName = this.inputSignatureFileNameArgument.getValue();
        }

        if(!this.senderPeerNameArgument.tryParse(arguments.get(2))) {
            this.getSharkMessengerApp().tellUIError("cannot parse sender name: " + arguments.get(2));
            return false;
        } else {
            this.senderPeerName = this.senderPeerNameArgument.getValue();
        }

        return true;
    }
}