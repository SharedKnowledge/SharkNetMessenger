package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TestScriptDescription {
    public final String ipAddress;
    public final int peerIndex;
    public final String script;
    public int testNumber;

    public TestScriptDescription(String ipAddress, int peerIndex, String script, int testNumber) {
        this.ipAddress = ipAddress;
        this.peerIndex = peerIndex;
        this.script = script;
        this.testNumber = testNumber;
    }

    public TestScriptDescription(byte[] serialisedMessage) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serialisedMessage);

        this.ipAddress = ASAPSerialization.readCharSequenceParameter(bais);
        this.peerIndex = ASAPSerialization.readIntegerParameter(bais);
        this.script = ASAPSerialization.readCharSequenceParameter(bais);
        this.testNumber = ASAPSerialization.readIntegerParameter(bais);
    }

    public byte[] getMessageBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ASAPSerialization.writeCharSequenceParameter(this.ipAddress, baos);
        ASAPSerialization.writeIntegerParameter(this.peerIndex, baos);
        ASAPSerialization.writeCharSequenceParameter(this.script, baos);
        ASAPSerialization.writeIntegerParameter(this.testNumber, baos);

        return baos.toByteArray();
    }

    public String toString() {
        return "ip: " + this.ipAddress + " | peerIndex: " + this.peerIndex + " | script: " + this.script;
    }
}
