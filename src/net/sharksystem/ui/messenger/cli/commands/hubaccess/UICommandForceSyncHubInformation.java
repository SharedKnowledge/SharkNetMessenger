package net.sharksystem.ui.messenger.cli.commands.hubaccess;

import net.sharksystem.SharkException;
import net.sharksystem.asap.utils.DateTimeHelper;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.peerside.HubConnector;
import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;
import net.sharksystem.ui.messenger.cli.commands.helper.Printer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class UICommandForceSyncHubInformation extends AbstractCommandNoParameter {
    public UICommandForceSyncHubInformation(SharkNetMessengerApp sharkMessengerApp,
                                            SharkNetMessengerUI smUI, String lsHubs, boolean b) {
        super(sharkMessengerApp, smUI, lsHubs, b);
    }

    @Override
    protected void execute() throws Exception {
        StringBuilder sb = new StringBuilder();
        HubConnectionManager hubConnectionManager = this.getSharkMessengerApp().getHubConnectionManager();

        hubConnectionManager.forceSync();

        this.getSharkMessengerApp().tellUI("forced hub connection manager to sync with all connected hubs");
    }

    @Override
    public String getDescription() {
        return "force hub connection manager getting intel on peers on connected hubs";
    }
}
