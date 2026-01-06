package net.sharksystem.ui.messenger.cli.distributedtesting;

import java.util.List;
import java.util.Map;

class OrchestratedTest {
    List<PeerHostingEnvironmentDescription> peerEnvironment;
    List<String> peerScripts;
    int maxDurationInMilli;
    String testName;

    OrchestratedTest(
            List<PeerHostingEnvironmentDescription> requiredPeerEnvironment,
            List<String> scripts,
            int maxDurationInMilli,
            String testName) {
        this.peerEnvironment = requiredPeerEnvironment;
        this.peerScripts = scripts;
        this.maxDurationInMilli = maxDurationInMilli;
        this.testName = testName;
    }
}
