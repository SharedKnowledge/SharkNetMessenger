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
        // 2025-12-03
        String script0_0 =  "sendMessage HiFromTest;wait 1000;lsMessages;";
        String script0_1 =  "sendMessage HiFromTest;wait 1000;lsMessages;";

        // Orchestrator: orchestrateTest dummy; openTCP 6907
        // dann Peers: connectTCP localhost 6907; scriptRQ

        // testing - seems orchestrator does not receive peerSettled release messages - it does sometimes, though.
        // Race condition - no doubt. S***.
        // check locks - getting even weirder - to test peer names are generated twice?
        // There is nothing but a good mystery :)
        // what the difference to scenario 0? no additional TCP connections(?)
        String script1_A =  "wait 5000;connectTCP localhost 9999;release A1;wait 5000;lsMessages;";
        String script1_B = "openTCP 9999;block A1;sendMessage HiFromBob;wait 5000;";

        /* effective scripts
        // message sent but not received but at least scenario runs - it is a race condition causing the problem above.
        o: openTCP 1984;block peerSettled_0_0;block peerSettled_0_1;release launchTest_0;;exit;
        a: connectTCP 192.168.0.116 1984;release peerSettled_0_0;block launchTest_0;wait 1000;wait 5000;connectTCP localhost 9999;release A1;wait 5000;lsMessages;;exit;
        b: connectTCP 192.168.0.116 1984;release peerSettled_0_1;block launchTest_0;wait 1000;openTCP 9999;block A1;sendMessage HiFromBob;wait 5000;;exit;
         */

        //List<PeerHostingEnvironmentDescription> requiredPeerEnvironment = new ArrayList<>();
        List<String> scripts = new ArrayList<>();

        // fill with example data
        this.getSharkMessengerApp().tellUI("use sample data - todo: fill with real data");
        // anything will do
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
