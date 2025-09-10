package net.sharksystem.ui.messenger.cli.commands.encounter;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;

public class UICommandDenyEncounter extends AbstractCommandNoParameter {
    public UICommandDenyEncounter(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                  String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
    }

    @Override
    protected void execute() throws Exception {
        /*
        ASAPEncounterManagerAdmin encounterManagerAdmin = this.getSharkMessengerApp().getEncounterManagerAdmin();
         */

        this.getSharkMessengerApp().tellUIError("not yet implemented: show encounter manager deny list");
    }

    @Override
    public String getDescription() {
        return "print open encounter";
    }
}
