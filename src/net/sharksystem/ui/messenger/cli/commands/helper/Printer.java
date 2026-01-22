package net.sharksystem.ui.messenger.cli.commands.helper;

import net.sharksystem.hub.ASAPHubException;
import net.sharksystem.hub.peerside.HubConnectorDescription;

import java.util.Iterator;

public class Printer {
    public static String getIntegerListAsCommaSeparatedString(Iterator<Integer> intIter) {
        if(intIter == null || !intIter.hasNext()) return "empty";

        StringBuilder sb = new StringBuilder();

        boolean first = true;
        do {
            if(first) first = false;
            else sb.append(", ");
            sb.append(intIter.next());
        } while(intIter.hasNext());

        return sb.toString();
    }

    public static String getStringListAsCommaSeparatedString(Iterator<CharSequence> csIter) {
        if(csIter == null || !csIter.hasNext()) return "empty";

        StringBuilder sb = new StringBuilder();

        boolean first = true;
        do {
            if(first) first = false;
            else sb.append(", ");
            sb.append(csIter.next());
        } while(csIter.hasNext());

        return sb.toString();
    }

    public static String getBooleanAsString(boolean b) {
        return b ? "true" : "false";
    }

    public static String getHubConnectorDescriptionAsString(HubConnectorDescription hcd) {
        if(hcd == null) return "";

        StringBuilder sb = new StringBuilder();
        // got all information - print it
        try {
            sb.append("host: ");
            sb.append(hcd.getHostName());
            sb.append(" | ");
            sb.append("port: ");
            sb.append(hcd.getPortNumber());
            sb.append(" | ");
            sb.append("can multichannel: ");
            sb.append(Printer.getBooleanAsString(hcd.canMultiChannel()));
            sb.append(" | ");
            sb.append("type: ");
            switch(hcd.getType()) {
                case 0x00 -> sb.append("TCP");
                default -> sb.append(hcd.getType());
            }
        } catch (ASAPHubException e) {
            sb.append("problems receiving hub descriptions: " + e.getLocalizedMessage());
        }
        return sb.toString();
    }
}
