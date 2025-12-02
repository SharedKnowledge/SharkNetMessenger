package net.sharksystem.ui.messenger.cli.commands.testing;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PeerHostingEnvironmentDescription {
    public final String ipAddress;
    public final String osName;
    public final String osVersion;
    public final String peerID;

    /** produce a description of this actual environment */
    public PeerHostingEnvironmentDescription(String peerID) throws UnknownHostException {
        this(
            InetAddress.getLocalHost().getHostAddress(), // IP Adresse
            System.getProperty("os.name"), // os name
            System.getProperty("os.version"), // os version
            peerID
        );
    }

    public PeerHostingEnvironmentDescription(String ipAddress, String osName, String osVersion, String peerID) {
        if(ipAddress == null) ipAddress = "";
        this.ipAddress = ipAddress;
        if(osName == null) osName = "";
        this.osName = osName;
        if(osVersion == null) osVersion = "";
        this.osVersion = osVersion;
        if(peerID == null) peerID = "";
        this.peerID = peerID;
    }

    public PeerHostingEnvironmentDescription(byte[] serializedMessage) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedMessage);

        this.ipAddress = ASAPSerialization.readCharSequenceParameter(bais);
        this.osName = ASAPSerialization.readCharSequenceParameter(bais);
        this.osVersion = ASAPSerialization.readCharSequenceParameter(bais);
        this.peerID = ASAPSerialization.readCharSequenceParameter(bais);
    }

    public byte[] getMessageBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ASAPSerialization.writeCharSequenceParameter(this.ipAddress, baos);
        ASAPSerialization.writeCharSequenceParameter(this.osName, baos);
        ASAPSerialization.writeCharSequenceParameter(this.osVersion, baos);
        ASAPSerialization.writeCharSequenceParameter(this.peerID, baos);

        return baos.toByteArray();
    }

    public String toString() {
        return "ip: " + this.ipAddress + " | os: " + this.osName + " | version: "
                + this.osVersion + " | name: " + this.peerID;
    }
}
