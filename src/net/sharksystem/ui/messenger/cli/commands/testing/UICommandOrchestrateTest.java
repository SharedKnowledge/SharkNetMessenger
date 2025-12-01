package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandWithSingleString;

import java.util.ArrayList;
import java.util.List;

public class UICommandOrchestrateTest extends AbstractCommandWithSingleString {
    SharkNetMessengerAppSupportingDistributedTesting snmTestSupport;

    public UICommandOrchestrateTest(SharkNetMessengerAppSupportingDistributedTesting sharkMessengerApp, SharkNetMessengerUI smUI, String echo, boolean b) {
        super(sharkMessengerApp, smUI, echo, b);
        this.snmTestSupport = sharkMessengerApp;
    }

    @Override
    protected void execute() throws Exception {
        /* need:
        * peers willing to run scripts
        * script for each peer
         */

        // Alice: orchestrateTest dummy; openTCP 9999
        // dann Bob: connectTCP localhost 9999; scriptRQ

        List<PeerHostingEnvironmentDescription> requiredPeerEnvironment = new ArrayList<>();
        List<String> scripts = new ArrayList<>();

        // fill with example data
        this.getSharkMessengerApp().tellUI("use sample data - todo: fill with real data");
        // anything will do
        requiredPeerEnvironment.add(new PeerHostingEnvironmentDescription(null, null, null));
        scripts.add("markstep RUN_TEST_:)");

        this.snmTestSupport.orchestrateTest(requiredPeerEnvironment, scripts);

        this.getSharkMessengerApp().tellUI("test is to be orchestrated");
    }

    @Override
    public String getDescription() {
        return "orchestrate test scenario";
    }
}
