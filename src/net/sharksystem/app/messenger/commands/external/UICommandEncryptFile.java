package net.sharksystem.app.messenger.commands.external;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandStringArgument;
import net.sharksystem.app.messenger.commands.pki.PKIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

public class UICommandEncryptFile extends UICommand {
    private final UICommandStringArgument inputFileNameArgument;
    private final UICommandStringArgument outputFileNameArgument;
    private final UICommandStringArgument targetPeerNameArgument;
    private String inputFileName;
    private String outputFileName;
    private String targetPeerName;

    public UICommandEncryptFile(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.inputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.outputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.targetPeerNameArgument = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("file name to encrypt: ", this.inputFileNameArgument)
                .addQuestion("output file name: ", this.outputFileNameArgument)
                .addQuestion("encrypt for whom (peer name): ", this.targetPeerNameArgument)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();
        Set<PersonValues> personValuesByName = null;

        // find person to rename
        PersonValues targetPeerValues = null;
        try {
            targetPeerValues = PKIUtils.getUniquePersonValues(this.targetPeerName, this.getSharkMessengerApp());
        }
        catch(ASAPException ae) {
            this.getSharkMessengerApp().tellUIError("no person found with name " + this.targetPeerName);
            return;
        }

        // got a peer
        // read file
        File file2encrypt = new File(this.inputFileName);
        long numberBytes2Encrypt = file2encrypt.length();
        if(numberBytes2Encrypt > Integer.MAX_VALUE) {
            this.getSharkMessengerApp().tellUIError("problem: file to long - more than MAX_INTEGER byte");
            return;
        }

        FileInputStream bytes2EncryptIS = new FileInputStream(this.inputFileName);
        OutputStream encryptedBytesOS = new FileOutputStream(this.outputFileName);

        // read content
        byte[] bytes2encrypt = new byte[(int) numberBytes2Encrypt];
        bytes2EncryptIS.read(bytes2encrypt);

        // produce package
        ASAPCryptoAlgorithms.writeEncryptedMessagePackage(
                bytes2encrypt, targetPeerValues.getUserID(), pki.getASAPKeyStore(), encryptedBytesOS);

        encryptedBytesOS.close();

        this.getSharkMessengerApp().tellUI("encrypted data written into file " + this.outputFileName);
    }

    @Override
    public String getDescription() {
        return "encrypt an extern file for a specific peer.";
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 3) {
            this.getSharkMessengerApp().tellUIError("required: file name (to encrypt), encrypted file name, receiving peer name");
            return false;
        }

        if(!this.inputFileNameArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse input file name: " + arguments.get(0));
            return false;
        } else {
            this.inputFileName = this.inputFileNameArgument.getValue();
        }

        if(!this.outputFileNameArgument.tryParse(arguments.get(1))) {
            this.getSharkMessengerApp().tellUIError("cannot parse output file name: " + arguments.get(1));
            return false;
        } else {
            this.outputFileName = this.outputFileNameArgument.getValue();
        }

        if(!this.targetPeerNameArgument.tryParse(arguments.get(2))) {
            this.getSharkMessengerApp().tellUIError("cannot parse receivers name: " + arguments.get(1));
            return false;
        } else {
            this.targetPeerName = this.targetPeerNameArgument.getValue();
        }

        return true;
    }
}