package net.sharksystem.ui.messenger.cli.commands.testing;

class Helper {
    public static final String SNM_CLI_JAR_FILENAME = "SharkNetMessengerCLI.jar";

    static String getFriendlyPeerName(String peerName) {
        if(peerName.length() != 1) return peerName; // already friendly - hopefully

        switch (peerName) {
            case "0":
            case "A":
                return "Alice";
            case "1":
            case "B":
                return "Bob";
            case "2":
            case "C":
                return "Clara";
            case "3":
            case "D":
                return "David";
            case "4":
            case "E":
                return "Eve";
            default: return peerName;
        }
    }
}
