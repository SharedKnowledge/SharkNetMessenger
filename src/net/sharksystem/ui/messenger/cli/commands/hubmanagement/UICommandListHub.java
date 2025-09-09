package net.sharksystem.ui.messenger.cli.commands.hubmanagement;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;
import net.sharksystem.ui.messenger.cli.commands.helper.Printer;

import java.util.Set;

public class UICommandListHub extends AbstractCommandNoParameter {
    public UICommandListHub(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI smUI, String lsHubs, boolean b) {
        super(sharkMessengerApp, smUI, lsHubs, b);
    }

    @Override
    protected void execute() throws Exception {
        Set<Integer> openHubPorts = this.getSharkMessengerApp().getOpenHubPorts();
        if(openHubPorts == null || openHubPorts.isEmpty()) {
            this.getSharkMessengerApp().tellUI("no asap hubs running in this process");
            return;
        }

        String sb = "Number of ASAP Hubs running in this process: " +
                openHubPorts.size() +
                "\nlistening on following TCP ports: " +
                Printer.getIntegerListAsCommaSeparatedString(openHubPorts.iterator());

        this.getSharkMessengerApp().tellUI(sb);
    }

    @Override
    public String getDescription() {
        return "list all hubs running locally";
    }
}
