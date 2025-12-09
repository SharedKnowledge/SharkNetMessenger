package net.sharksystem.app.messenger.commands.external;

import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandStringArgument;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class UICommandDecryptFile extends UICommand {
    private final UICommandStringArgument inputFileNameArgument;
    private final UICommandStringArgument outputFileNameArgument;
    private String inputFileName;
    private String outputFileName;

    public UICommandDecryptFile(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.inputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.outputFileNameArgument = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("file name to decrypt: ", this.inputFileNameArgument)
                .addQuestion("output file name: ", this.outputFileNameArgument)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();

        FileInputStream encryptedFileIS = new FileInputStream(this.inputFileName);

        // open file for writing decrypted data
        OutputStream decryptedFileOS = new FileOutputStream(this.outputFileName);

        // write decrypted data
        decryptedFileOS.write(
                // decrypt package
                ASAPCryptoAlgorithms.decryptPackage(
                    // read encrypted package from file
                    ASAPCryptoAlgorithms.parseEncryptedMessagePackage(encryptedFileIS),
                    pki.getASAPKeyStore())
        );

        decryptedFileOS.close();
        encryptedFileIS.close();

        this.getSharkMessengerApp().tellUI("decrypted data written into file " + this.outputFileName);
    }

    @Override
    public String getDescription() {
        return "decrypt an extern file decrypted for this peer.";
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 2) {
            this.getSharkMessengerApp().tellUIError("required: encrypted file name, file name for decrypted data");
            return false;
        }

        if(!this.inputFileNameArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse encrypted file name: " + arguments.get(0));
            return false;
        } else {
            this.inputFileName = this.inputFileNameArgument.getValue();
        }

        if(!this.outputFileNameArgument.tryParse(arguments.get(1))) {
            this.getSharkMessengerApp().tellUIError("cannot parse file name for decrypted data: " + arguments.get(1));
            return false;
        } else {
            this.outputFileName = this.outputFileNameArgument.getValue();
        }
        return true;
    }
}