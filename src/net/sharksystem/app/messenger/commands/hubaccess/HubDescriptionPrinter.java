package net.sharksystem.app.messenger.commands.hubaccess;

import net.sharksystem.hub.ASAPHubException;
import net.sharksystem.hub.HubConnectionManager;
import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.io.PrintStream;
import java.util.List;

public class HubDescriptionPrinter {
    public static void print(PrintStream ps, List<HubConnectorDescription> hcdList) {
        if(hcdList == null || hcdList.isEmpty()) {
            ps.println("no hub descriptions available");
        }

        boolean first = true;
        int index = 0;
        for(HubConnectorDescription hcd : hcdList) {
            if(!first) ps.print('\n');
            else first = false;
            ps.print(index++ + ": ");
            HubDescriptionPrinter.print(ps, hcd);
        }
    }

    public static void print(PrintStream ps, HubConnectorDescription hcd) {
        try {
            CharSequence hostname = hcd.getHostName();
            int portNumber = hcd.getPortNumber();
            boolean canMultichannel = hcd.canMultiChannel();
            byte type = hcd.getType();

            // got all information - print it
            ps.print("host: ");
            ps.print(hostname);
            ps.print(" | ");
            ps.print("port: ");
            ps.print(portNumber);
            ps.print(" | ");
            ps.print("can multichannel: ");
            if(canMultichannel) ps.print("true"); else ps.print("false");
            ps.print(" | ");
            ps.print("type: ");
            ps.print(type);
        } catch (ASAPHubException e) {
            ps.println("problems receiving hub descriptions: " + e.getLocalizedMessage());
        }
    }

    public static void printConnectedHubs(PrintStream ps, HubConnectionManager hubConnectionManager) {
        ps.println("connected hubs:");
        print(ps, hubConnectionManager.getConnectedHubs());
    }
}
