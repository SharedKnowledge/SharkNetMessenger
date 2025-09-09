package net.sharksystem.ui.messenger.cli.commands.external;

import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class UICommandProduceSignature extends UICommand {
    private final UICommandStringArgument inputFileNameArgument;
    private final UICommandStringArgument outputFileNameArgument;
    private String inputFileName;
    private String outputFileName;

    public static final String SIGNATURE_EXTENSION = "_signedBy_";


    public UICommandProduceSignature(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                     String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.inputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.outputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("file name to sign: ", this.inputFileNameArgument)
                .addQuestion("signature file name: ", this.outputFileNameArgument)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();

        // read file
        File file2encrypt = new File(this.inputFileName);
        long numberBytes2Encrypt = file2encrypt.length();
        if(numberBytes2Encrypt > Integer.MAX_VALUE) {
            this.getSharkMessengerApp().tellUIError("problem: file to long - more than MAX_INTEGER byte");
            return;
        }

        FileInputStream bytes2SignIS = new FileInputStream(this.inputFileName);
        FileOutputStream signedFileOS = new FileOutputStream(this.outputFileName);

        // read content
        byte[] bytes2sign = new byte[(int) numberBytes2Encrypt];
        bytes2SignIS.read(bytes2sign);

        // produce signature
        byte[] signature = ASAPCryptoAlgorithms.sign(bytes2sign, pki.getASAPKeyStore());

        // write signature into signature file
        signedFileOS.write(signature);

        this.getSharkMessengerApp().tellUI("signature written into file " + this.outputFileName);
    }

    @Override
    public String getDescription() {
        return "encrypt an extern file for a specific peer.";
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 1) {
            this.getSharkMessengerApp().tellUIError(
                    "required: file name (to sign), signed file (default: add " +
                            SIGNATURE_EXTENSION + ")");
            return false;
        }

        if(!this.inputFileNameArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse input file name: " + arguments.get(0));
            return false;
        } else {
            this.inputFileName = this.inputFileNameArgument.getValue();
        }

        if(arguments.size() > 1) {
            if(!this.outputFileNameArgument.tryParse(arguments.get(1))) {
                this.getSharkMessengerApp().tellUIError("cannot parse signature file name: " + arguments.get(1));
                return false;
            } else {

                this.outputFileName = this.outputFileNameArgument.getValue();
            }
        } else { // default
            int lastDotIndex = this.inputFileName.lastIndexOf('.');
            // inputFileName.extension
            //      lastDot ^
            // assume: no dot
            String beforeDot = this.inputFileName;
            String afterDot = "";
            if (lastDotIndex > 0) {
                beforeDot = this.inputFileName.substring(0, lastDotIndex);
                afterDot = this.inputFileName.substring(lastDotIndex);
            }

            this.outputFileName =
                    beforeDot + SIGNATURE_EXTENSION + this.getSharkMessengerApp().getPeerName() + afterDot;
        }

        return true;
    }
}