package net.sharksystem.ui.messenger.cli.distributedtesting;

import java.util.List;

class OrchestratedTest {
    List<PeerHostingEnvironmentDescription> peerEnvironment;
    List<String> scripts;
    int maxDurationInMilli;
    String testName;

    OrchestratedTest(
            List<PeerHostingEnvironmentDescription> requiredPeerEnvironment,
            List<String> scripts,
            int maxDurationInMilli,
            String testName) {
        this.peerEnvironment = requiredPeerEnvironment;
        this.scripts = scripts;
        this.maxDurationInMilli = maxDurationInMilli;
        this.testName = testName;
    }
}
