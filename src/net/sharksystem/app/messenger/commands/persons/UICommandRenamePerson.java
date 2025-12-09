package net.sharksystem.app.messenger.commands.persons;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.persons.PersonValues;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandStringArgument;

import java.util.List;
import java.util.Set;

public class UICommandRenamePerson extends UICommand {
    private final UICommandStringArgument personNameArgument;
    private final UICommandStringArgument personNewNameArgument;
    private String personName;
    private String personNewName;

    public UICommandRenamePerson(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                 String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.personNameArgument = new UICommandStringArgument(sharkMessengerApp);
        this.personNewNameArgument = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("old name: ", this.personNameArgument)
                .addQuestion("new name: ", this.personNewNameArgument)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkPKIComponent pki = this.getSharkMessengerApp().getSharkPKIComponent();
        Set<PersonValues> personValuesByName = null;

        // find person to rename
        PersonValues person2Rename = null;
        try {
            personValuesByName = pki.getPersonValuesByName(this.personName);
            if(personValuesByName.size() > 1) {
                this.getSharkMessengerApp().tellUI("problem: more than one persons found with name " + this.personName);
                return;
            }
            person2Rename = personValuesByName.iterator().next();
        }
        catch(ASAPException ae) {
            this.getSharkMessengerApp().tellUI("no person found with name " + this.personName);
            return;
        }

        // we have someone to rename, next: name already taken?
        try {
            personValuesByName = pki.getPersonValuesByName(this.personNewName);
            if(personValuesByName.size() > 0) {
                this.getSharkMessengerApp().tellUI("problem: name already taken: " + this.personNewName);
                return;
            }
        }
        catch(ASAPException ae) {
            // that's okay - name is not in use
        }

        person2Rename.setName(this.personNewName);
        pki.saveMemento();
        this.getSharkMessengerApp().tellUI("changed name from " + this.personName + " to " + this.personNewName);
    }

    @Override
    public String getDescription() {
        return "change person's name.";
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
        if(arguments.size() < 2) {
            this.getSharkMessengerApp().tellUIError("required: old name, new name");
            return false;
        }

        if(!this.personNameArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("cannot parse name: " + arguments.get(0));
            return false;
        } else {
            this.personName = this.personNameArgument.getValue();
        }

        if(!this.personNewNameArgument.tryParse(arguments.get(1))) {
            this.getSharkMessengerApp().tellUIError("cannot parse new name: " + arguments.get(1));
            return false;
        } else {
            this.personNewName = this.personNewNameArgument.getValue();
        }

        return true;
    }
}