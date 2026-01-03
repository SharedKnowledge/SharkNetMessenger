package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerAppSupportingDistributedTesting;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.app.messenger.commands.helper.AbstractCommandWithSingleString;
import net.sharksystem.utils.json.JSONObject;
import net.sharksystem.utils.json.JSONParser;

import java.io.File;
import java.io.IOException;
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
        // get script file
        String fileName = this.getStringArgument();
        JSONParser jsonParser = new JSONParser(new File(fileName));

        // get all scripts - mandatory parameter
        JSONObject testCaseDescription = jsonParser.getParsedDocument();
        JSONObject scripts = testCaseDescription.getValue(OrchestrationScriptConstants.SCRIPTS_NAME);
        List<String> scriptsList = scripts.getStringValueList();

        if(scriptsList.isEmpty()) {
            this.getSharkMessengerApp().tellUIError("scripts list must not be empty");
            return;
        }

        //////////////////////// TEST NAME
        String testName;
        try {
            testName = testCaseDescription.getValue(OrchestrationScriptConstants.TESTNAME_NAME).getValue();
        }
        catch(IOException io) {
            // no testname set - create on based on filename - TODO
            int indexDot = fileName.indexOf(".");
            if(indexDot == -1) {
                testName = fileName;
            } else {
                testName = fileName.substring(indexDot);
            }
            this.getSharkMessengerApp().tellUI("no testname set - take file name: " + fileName);
        }

        //////////////////////// MAX DURATION
        int maxDuration = OrchestrationScriptConstants.DEFAULT_MAX_DURATION_IN_MS;
        try {
            String maxDurationString =
                    testCaseDescription.getValue(OrchestrationScriptConstants.MAX_DURATION_IN_MS_NAME).getValue();

            maxDuration = Integer.parseInt(maxDurationString);
        }
        catch(NumberFormatException | IOException e) {
            this.getSharkMessengerApp().tellUI("max duration not explicitly set - go with default");
        }

        //////////////////////// PEER REQUIREMENTS
        JSONObject peerRequirements =
                testCaseDescription.getValue(OrchestrationScriptConstants.PEER_ENVIRONMENT_REQUIREMENTS_NAME);

        // read peer environment requirements
        List<JSONObject> peerRequirementList = peerRequirements.getObjectsList();
        if(peerRequirementList.size() > scriptsList.size()) {
            this.getSharkMessengerApp().tellUIError("peer requirements list is longer than scripts list - give up");
            return;
        }

        List<PeerHostingEnvironmentDescription> peerRequirementsDescriptionList = new ArrayList<>();

        for(JSONObject requirements : peerRequirementList) {
            String osName = null;
            String osVersion = null;
            try {
                JSONObject element =
                        requirements.getValue(OrchestrationScriptConstants.PEER_ENVIRONMENT_REQUIREMENTS_OS_NAME);
                osName = element.getValue();
            }
            catch(IOException ioe) { /* no osName - that's okay */ }
            try {
                JSONObject element =
                        requirements.getValue(OrchestrationScriptConstants.PEER_ENVIRONMENT_REQUIREMENTS_OS_VERSION);
                osVersion = element.getValue();
            }
            catch(IOException ioe) { /* no osVersion - that's okay */ }

            if((osName == null || osName.isEmpty()) && (osVersion == null || osVersion.isEmpty())) {
                this.getSharkMessengerApp().tellUI("at least one peer requirements description is present but does not contain any requirements - ignored");
            } else {
                peerRequirementsDescriptionList.add(
                   new PeerHostingEnvironmentDescription(null, osName, osVersion, null));
            }
        }

        // Orchestrator: orchestrateTest ..\csTestCase.txt; openTCP 6907
        // dann Peers: connectTCP localhost 6907; scriptRQ

        /**
         * Funktioniert.... Es gibt nun P1 und P1_0 etc.
         * Außerdem... Füge die Parameter für die Peers hinzu... Vergib gern Namen für die Peers.
         * lass im Konfig file auch zu, wie lange die TimeBomb ticken soll.
         */

        this.getSharkMessengerApp().
                tellUI("waiting for peers \n" + peerRequirementsDescriptionList + "\nto execute:\n" + scriptsList);

        // fill with example data
        this.snmTestSupport.orchestrateTest(
                peerRequirementsDescriptionList,
                scriptsList,
                maxDuration,
                testName);
    }

    @Override
    public String getDescription() {
        return "orchestrate test scenario";
    }

}
