package net.sharksystem.ui.messenger.cli.commands.testing;

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

        /**
         * what works - independent tests - no interaction between test peers
         * Test with one peer... test is executed - results are persistent in test peer
         * Test with two peers... no script is executed, Clara even claims not to be able to stage a test.
         */

        // Orchestrator: orchestrateTest dummy; openTCP 9999
        // dann Peers: connectTCP localhost 9999; scriptRQ

        String script1_0 =  "connectTCP localhost 9999;release A1;wait 5000;lsMessages;";
        String script1_1 = "openTCP 9999;block A1;sendMessage HiFromBob;wait 5000;";

        // works: 2025.12.03
        String script0_0 =  "sendMessage HiFromTest;wait 1000;lsMessages;";
        String script0_1 =  "sendMessage HiFromTest;wait 1000;lsMessages;";

        //List<PeerHostingEnvironmentDescription> requiredPeerEnvironment = new ArrayList<>();
        List<String> scripts = new ArrayList<>();

        // fill with example data
        this.getSharkMessengerApp().tellUI("use sample data - todo: fill with real data");
        // anything will do
        scripts.add(script0_0);
        scripts.add(script0_1);

        this.snmTestSupport.orchestrateTest(scripts);

        this.getSharkMessengerApp().tellUI("test is to be orchestrated");
    }

    @Override
    public String getDescription() {
        return "orchestrate test scenario";
    }
}
