package net.sharksystem.ui.messenger.cli.distributedtesting;

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

    /**
     * Matching means: osName and osVersion fit to each other. ipAddress and peerID is (not yet) of any concern
     * @param requiredEnvironment demands
     * @param availableEnvironment given
     * @return matches or not
     */
    public static boolean match(PeerHostingEnvironmentDescription requiredEnvironment,
                                PeerHostingEnvironmentDescription availableEnvironment) {
        boolean match = true;

        // os required? if so - does available environment match?
        if (requiredEnvironment.osName != null && !requiredEnvironment.osName.isEmpty()) {
            // yes os is demanded.
            if (!requiredEnvironment.osName.equalsIgnoreCase(availableEnvironment.osName)) {
                match = false;
            }
        }

        // version required? if so - does available environment match?
        if(match && requiredEnvironment.osVersion != null && !requiredEnvironment.osVersion.isEmpty()) {
            if (!requiredEnvironment.osVersion.equalsIgnoreCase(availableEnvironment.osVersion)) {
                match = false;
            }
        }

        return match;
    }

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
