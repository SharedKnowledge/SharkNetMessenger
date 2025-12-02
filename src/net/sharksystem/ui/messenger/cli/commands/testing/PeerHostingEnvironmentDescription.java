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

    /** produce a description of this actual environment */
    public PeerHostingEnvironmentDescription() throws UnknownHostException {
        this(
            InetAddress.getLocalHost().getHostAddress(), // IP Adresse
            System.getProperty("os.name"), // os name
            System.getProperty("os.version") // os version
        );
    }

    PeerHostingEnvironmentDescription(String ipAddress, String osName, String osVersion) {
        this.ipAddress = ipAddress;
        this.osName = osName;
        this.osVersion = osVersion;
    }

    public PeerHostingEnvironmentDescription(byte[] serializedMessage) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedMessage);

        this.ipAddress = ASAPSerialization.readCharSequenceParameter(bais);
        this.osName = ASAPSerialization.readCharSequenceParameter(bais);
        this.osVersion = ASAPSerialization.readCharSequenceParameter(bais);
    }

    byte[] getMessageBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ASAPSerialization.writeCharSequenceParameter(this.ipAddress, baos);
        ASAPSerialization.writeCharSequenceParameter(this.osName, baos);
        ASAPSerialization.writeCharSequenceParameter(this.osVersion, baos);

        return baos.toByteArray();
    }

    public String toString() {
        return "ip: " + this.ipAddress + " | os: " + this.osName + " | version: " + this.osVersion;
    }
}
