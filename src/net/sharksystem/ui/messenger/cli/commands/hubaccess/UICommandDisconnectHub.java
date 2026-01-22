package net.sharksystem.ui.messenger.cli.commands.hubaccess;

import java.util.List;

import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandIntegerArgument;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.UICommand;

public class UICommandDisconnectHub extends UICommand {
    private final UICommandIntegerArgument hubIndex;

    /**
     * Creates a command object.
     *
     * @param sharkMessengerApp
     * @param sharkMessengerUI
     * @param identifier        The identifier of the command.
     * @param rememberCommand   If the command should be saved in the history log.
     */
    public UICommandDisconnectHub(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI, String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        this.hubIndex = new UICommandIntegerArgument(sharkMessengerApp);
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        return null;
    }

    @Override
    protected void execute() throws Exception {
        List<HubConnectorDescription> connectedHubs =
                this.getSharkMessengerApp().getHubConnectionManager().getConnectedHubs();
        if(connectedHubs.size() < this.hubIndex.getValue()) {
            this.getSharkMessengerApp().tellUIError("index too high: " + this.hubIndex.getValue());
        }
        this.getSharkMessengerApp().getHubConnectionManager().disconnectHub(this.hubIndex.getValue());
        this.getSharkMessengerApp().tellUI("..done");
    }

    @Override
    public String getDescription() {
        return "disconnects from a running hub";
    }

    /**
     * This command requires no arguments.
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            this.getSharkMessengerApp().tellUIError("hub index required - find it be producing a list of connected hubs.");
            return false;
        }

        if(!this.hubIndex.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("could not parse hub index:" + arguments.get(0));
            return false;
        }

        if(this.hubIndex.getValue() < 1) {
            this.getSharkMessengerApp().tellUIError("index must be 1 or higher" + arguments.get(0));
            return false;
        }
        this.hubIndex.setValue(this.hubIndex.getValue()-1);
        return true;
    }
}
