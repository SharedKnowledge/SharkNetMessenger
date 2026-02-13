package net.sharksystem.ui.messenger.cli.distributedtesting;

import java.util.List;

class TestEnsemble {
    final Integer waitingTestIndex;
    final List<PeerHostingEnvironmentDescription> peerEnvironment;

    TestEnsemble(Integer waitingTestIndex, List<PeerHostingEnvironmentDescription> peerEnvironment) {
        this.waitingTestIndex = waitingTestIndex;
        this.peerEnvironment = peerEnvironment;
    }
}
