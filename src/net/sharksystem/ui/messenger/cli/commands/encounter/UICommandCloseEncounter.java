package net.sharksystem.ui.messenger.cli.commands.encounter;

import net.sharksystem.asap.ASAPEncounterManagerAdmin;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class UICommandCloseEncounter extends UICommand {
    private final UICommandStringArgument indexOrID;
    private CharSequence peerID = null;
    private int index = -1;

    public UICommandCloseEncounter(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                   String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.indexOrID = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        return null;
    }
    @Override
    protected void execute() throws Exception {
        ASAPEncounterManagerAdmin encounterManagerAdmin = this.getSharkMessengerApp().getEncounterManagerAdmin();

        if(this.index > -1) {
            Iterator<CharSequence> iterator = encounterManagerAdmin.getConnectedPeerIDs().iterator();
            if(!iterator.hasNext()) {
                this.getSharkMessengerApp().tellUIError("there is no encounter.");
                return;
            }
            int i = 0;
            do {
                i++;
                try {
                    this.peerID = iterator.next();
                }
                catch(NoSuchElementException e) {
                    this.getSharkMessengerApp().tellUIError("no encounter with index "+ this.index);
                }
            } while(i < this.index);
        }

        this.getSharkMessengerApp().tellUI("close encounter with " + this.peerID);
        encounterManagerAdmin.closeEncounter(this.peerID);
    }

    @Override
    public String getDescription() {
        return "stop encounter (index from encounter list OR peerID";
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            this.getSharkMessengerApp().tellUIError("index from encounter list or peerID required.");
            return false;
        }

        String stringArgument = arguments.get(0);
        try {
            this.index = Integer.parseInt(stringArgument);
        }
        catch(NumberFormatException e) {
            this.getSharkMessengerApp().tellUI("handle parameter as peerID");
            this.peerID = stringArgument;
        }

        return true;
    }
}
