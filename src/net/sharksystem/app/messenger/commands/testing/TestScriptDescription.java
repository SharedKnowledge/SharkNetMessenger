package net.sharksystem.app.messenger.commands.testing;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TestScriptDescription {
    public final String ipAddress;
    public final int peerIndex;
    public final String script;
    public final int testNumber;
    public final String peerID;

    public static boolean same(TestScriptDescription a, TestScriptDescription b) {
        return a.testNumber == b.testNumber
                && a.peerIndex == b.peerIndex
                && a.ipAddress.equalsIgnoreCase(b.ipAddress)
                && a.peerID.equalsIgnoreCase(b.peerID);
    }

    public TestScriptDescription(String ipAddress, int peerIndex, String script, int testNumber, String peerID) {
        this.ipAddress = ipAddress;
        this.peerIndex = peerIndex;
        this.script = script;
        this.testNumber = testNumber;
        this.peerID = peerID;
    }

    public TestScriptDescription(byte[] serialisedMessage) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serialisedMessage);

        this.ipAddress = ASAPSerialization.readCharSequenceParameter(bais);
        this.peerIndex = ASAPSerialization.readIntegerParameter(bais);
        this.script = ASAPSerialization.readCharSequenceParameter(bais);
        this.testNumber = ASAPSerialization.readIntegerParameter(bais);
        this.peerID = ASAPSerialization.readCharSequenceParameter(bais);
    }

    public byte[] getMessageBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ASAPSerialization.writeCharSequenceParameter(this.ipAddress, baos);
        ASAPSerialization.writeIntegerParameter(this.peerIndex, baos);
        ASAPSerialization.writeCharSequenceParameter(this.script, baos);
        ASAPSerialization.writeIntegerParameter(this.testNumber, baos);
        ASAPSerialization.writeCharSequenceParameter(this.peerID, baos);

        return baos.toByteArray();
    }

    public String toString() {
        return "ip: " + this.ipAddress + " | peerIndex: " + this.peerIndex
                + " | peerID: " + this.peerID
                + " | test#: " + this.testNumber
                + " | script: " + this.script;
    }
}
