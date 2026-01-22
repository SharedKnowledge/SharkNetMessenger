package net.sharksystem.ui.messenger.cli.commands.persons;

import java.util.List;
import java.util.Set;

import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.*;

public class UICommandSetSigningFailure extends UICommand {
    private final UICommandStringArgument subjectNameArgument;
    private final UICommandIntegerArgument signingFailureArgument;
    private String subjectName;
    private int newSigningFailure;

    public UICommandSetSigningFailure(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                      String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.subjectNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.signingFailureArgument = new UICommandIntegerArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("Subject peer name: ", this.subjectNameArgument)
                .addQuestion("Failure rate: ", this.signingFailureArgument)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();
        Set<PersonValues> personValuesByName = pki.getPersonValuesByName(this.subjectName);
        if(personValuesByName == null || personValuesByName.isEmpty()) {
            this.getSharkMessengerApp().tellUI("no person found with name " + this.subjectName);
            return;
        }

        if(personValuesByName.size() > 1) {
            this.getSharkMessengerApp().tellUI("problem: more than one persons found with name " + this.subjectName);
            return;
        }

        pki.setSigningFailureRate(personValuesByName.iterator().next().getUserID(), this.newSigningFailure);
        this.getSharkMessengerApp().tellUI("ok - set new signing failure of " + this.subjectName + " to " + this.newSigningFailure);
    }

    @Override
    public String getDescription() {
        return "Sets the signing failure rate for a specific peer.";
    }

    /**
     * @param arguments in following order:
     * <ol>
     *  <li>subject - peerID</li>
     *  <li>failureRate - int</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 1) {
            this.getSharkMessengerApp().tellUIError("required: peer name, new signing Failure (1-10)");
            return false;
        }

        if(!this.subjectNameArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse name: " + arguments.get(0));
            return false;
        } else {
            this.subjectName = this.subjectNameArgument.getValue();
        }

        this.newSigningFailure = 5; // set default
        if(arguments.size() > 1) {
            if(this.signingFailureArgument.tryParse(arguments.get(1))) {
                // overwrite default
                this.newSigningFailure = this.signingFailureArgument.getValue();
            }
        }

        return true;
    }
}