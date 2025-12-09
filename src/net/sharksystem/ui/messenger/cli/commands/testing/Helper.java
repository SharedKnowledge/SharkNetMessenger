package net.sharksystem.ui.messenger.cli.commands.testing;

class Helper {
    public static final String SNM_CLI_JAR_FILENAME = "SharkNetMessengerCLI.jar";

    static String getFriendlyPeerName(String peerName) {
        if(peerName.length() != 1) return peerName; // already friendly - hopefully

        switch (peerName) {
            case "0":
            case "A":
                return "P1";
            case "1":
            case "B":
                return "P2";
            case "2":
            case "C":
                return "P3";
            case "3":
            case "D":
                return "P4";
            case "4":
            case "E":
                return "P5";
            default: return peerName;
        }
    }
}
