package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleString;
import net.sharksystem.utils.json.JSONObject;
import net.sharksystem.utils.json.JSONParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UICommandOrchestrateTest extends AbstractCommandWithSingleString {
    SharkNetMessengerAppSupportingDistributedTesting snmTestSupport;

    public UICommandOrchestrateTest(SharkNetMessengerAppSupportingDistributedTesting sharkMessengerApp, SharkNetMessengerUI smUI, String echo, boolean b) {
        super(sharkMessengerApp, smUI, echo, b);
        this.snmTestSupport = sharkMessengerApp;
    }

    /* Scripts.txt example
   {
"PeerRequirements":
[]

"Scripts":
[
"1": "connectTCP localhost 4444;release CS;sendMessage HiFromAlice;lsMessages;"
"2": "openTCP 4444;block CS;lsMessages;wait 1000;lsMessages;"
]
}
     */

    @Override
    protected void execute() throws Exception {
        /* need:
        * peers willing to run scripts
        * script for each peer
         */
        // testscripts
        /*
        String script0_A =  "sendMessage HiFromTest;wait 1000;lsMessages;";
        String script0_B =  "sendMessage HiFromTest;wait 1000;lsMessages;";

        // going to launch new processes rather new threads: orchestrator (not yet); test peers (not yet)
        String script1_A =  "connectTCP localhost 4444;release CS;sendMessage HiFromAlice;lsMessages;";
        String script1_B = "openTCP 4444;block CS;lsMessages;wait 1000;lsMessages;";
         */

        // get script file
        JSONParser jsonParser = new JSONParser(new File(this.getStringArgument()));

        // get all scripts - mandatory parameter
        JSONObject scripts = jsonParser.getParsedDocument().getValue("Scripts");
        List<String> scriptsList = scripts.getStringValueList();

        // later
        JSONObject peerRequirements = jsonParser.getParsedDocument().getValue("PeerRequirements");
        // TODO
        List<String> peerRequirementList = peerRequirements.getStringValueList();

        // Orchestrator: orchestrateTest scripts.txt; openTCP 6907
        // dann Peers: connectTCP localhost 6907; scriptRQ

        // fill with example data
        this.getSharkMessengerApp().tellUI("peer requirements are ignored yet - todo");

        this.snmTestSupport.orchestrateTest(scriptsList);

        this.getSharkMessengerApp().tellUI("waiting for peers to execute:" + scriptsList);
    }

    @Override
    public String getDescription() {
        return "orchestrate test scenario";
    }

}
