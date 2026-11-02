package net.sharksystem.ui.messenger.cli.distributedtesting;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class TestScriptDescription {
    public final String ipAddress;
    public final int peerIndex;
    public final String script;
    public final String testID;
    public final String peerID;
    public final String orchestratorAddress;
    public final int orchestratorPort;
    public final int maxDurationInMillis;

    public static boolean same(TestScriptDescription a, TestScriptDescription b) {
        return a.peerIndex == b.peerIndex
                && a.ipAddress.equalsIgnoreCase(b.ipAddress)
                && a.peerID.equalsIgnoreCase(b.peerID)
                && a.testID.equalsIgnoreCase(b.testID);
    }

    public TestScriptDescription(String ipAddress, int peerIndex, String script, String testID, String peerID,
             String orchestratorAddress, int orchestratorPort, int maxDurationInMillis) {
        this.ipAddress = ipAddress;
        this.peerIndex = peerIndex;
        this.script = script;
        this.testID = testID;
        this.peerID = peerID;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorPort = orchestratorPort;
        this.maxDurationInMillis = maxDurationInMillis;
    }

    public TestScriptDescription(byte[] serialisedMessage) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serialisedMessage);

        this.ipAddress = ASAPSerialization.readCharSequenceParameter(bais);
        this.peerIndex = ASAPSerialization.readIntegerParameter(bais);
        this.script = ASAPSerialization.readCharSequenceParameter(bais);
        this.testID = ASAPSerialization.readCharSequenceParameter(bais);
        this.peerID = ASAPSerialization.readCharSequenceParameter(bais);
        this.orchestratorAddress = ASAPSerialization.readCharSequenceParameter(bais);
        this.orchestratorPort = ASAPSerialization.readIntegerParameter(bais);
        this.maxDurationInMillis = ASAPSerialization.readIntegerParameter(bais);
    }

    public byte[] getMessageBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ASAPSerialization.writeCharSequenceParameter(this.ipAddress, baos);
        ASAPSerialization.writeIntegerParameter(this.peerIndex, baos);
        ASAPSerialization.writeCharSequenceParameter(this.script, baos);
        ASAPSerialization.writeCharSequenceParameter(this.testID, baos);
        ASAPSerialization.writeCharSequenceParameter(this.peerID, baos);
        ASAPSerialization.writeCharSequenceParameter(this.orchestratorAddress, baos);
        ASAPSerialization.writeIntegerParameter(this.orchestratorPort, baos);
        ASAPSerialization.writeIntegerParameter(this.maxDurationInMillis, baos);

        return baos.toByteArray();
    }

    public String toString() {
        return "ip: " + this.ipAddress + " | peerIndex: " + this.peerIndex
                + " | peerID-Launcher: " + this.peerID
                + " | testID: " + this.testID
                + " | max in ms: " + this.maxDurationInMillis
                + " | o-host: " + this.orchestratorAddress
                + " | o-port: " + this.orchestratorPort;
    }
}
