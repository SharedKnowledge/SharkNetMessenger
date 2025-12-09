package net.sharksystem.app.messenger.commands.hubaccess;

import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandNoParameter;

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
