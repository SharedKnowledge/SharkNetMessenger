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

         // what works - independent tests - no interaction between test peers
        // 2025-12-03 with thread variant
        // 2025-12-05 works with process builder
        String script0_A =  "sendMessage HiFromTest;wait 1000;lsMessages;";
        String script0_B =  "sendMessage HiFromTest;wait 1000;lsMessages;";

        // going to launch new processes rather new threads: orchestrator (not yet); test peers (not yet)
        String script1_A =  "wait 2000;connectTCP localhost 9999;release A1;wait 1000;lsMessages;";
        String script1_B = "openTCP 9999;block A1;sendMessage HiFromBob;";

        //List<PeerHostingEnvironmentDescription> requiredPeerEnvironment = new ArrayList<>();
        List<String> scripts = new ArrayList<>();

        // Orchestrator: orchestrateTest dummy; openTCP 6907
        // dann Peers: connectTCP localhost 6907; scriptRQ

        // fill with example data
        this.getSharkMessengerApp().tellUI("use sample data - todo: fill with real data");
        // anything will do
        /*
        scripts.add(script0_A);
        scripts.add(script0_B);
         */

        scripts.add(script1_A);
        scripts.add(script1_B);

        this.snmTestSupport.orchestrateTest(scripts);

        this.getSharkMessengerApp().tellUI("test is to be orchestrated");
    }

    @Override
    public String getDescription() {
        return "orchestrate test scenario";
    }
}
