package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.CommandListToFile;

public class CoreScenariosHub {

    public static final String START_HUB = "startHub";
    private String ipHub = "localhost";
    public static final String CONNECT_HUB = "connectHub";
    public static final String DISCONNECT_HUB = "disconnectHub";
    public static final String DISCONNECT_HUB_LINE = DISCONNECT_HUB + " 1";
    private int hubPort = 6907;

    public CoreScenariosHub() {}

    /**
     * Constructor for CoreScenariosHub.
     */
    public CoreScenariosHub(int hubStartingPort, String hubIPAddress) {
        this.ipHub = hubIPAddress;
        this.hubPort = hubStartingPort;
    }

    /**
     * Generates command list for starting the hub.
     * @return the command list as a string
     */
    public final String hubHostCommand() {
        return START_HUB + " " + ipHub + " " + hubPort
                + System.lineSeparator();
    }

    /**
     * Generates command list for a peer to connect to the hub before the other peer.
     * @return the command list as a string
     */
    public String hubCoreCommands(int order) {
        if (order > 2 || order < 1)
                throw new IllegalArgumentException("Order must be 1 or 2.");

        String peerPrimary = CONNECT_HUB + " " + ipHub + " " + hubPort
                + System.lineSeparator();

        String peerSecondary = CommandListToFile.WAIT
                + " " + 500
                + System.lineSeparator()
                + peerPrimary;

        return order == 1 ? peerPrimary : peerSecondary;
    }

    public String disconnectFromHub() {
        return CommandListToFile.WAIT + " " + 500
                + DISCONNECT_HUB_LINE
                + System.lineSeparator();
    }

    public String disconnectFromHub(int hubIndex) {
        return DISCONNECT_HUB + " " + hubIndex
                + System.lineSeparator();
    }

    public String hubACommands(int order) {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");

        String peerSending = hubCoreCommands(order)
                    + CommandListToFile.SEND_MESSAGE + "HUB_A" + " " + "sn/char";

        return order == 1 ? peerSending : hubCoreCommands(order);
    }

    public String hubB1Commands(int order) {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");

        String peerSending = CommandListToFile.SEND_MESSAGE + "HUB_B1" + " " + "sn/char"
                + System.lineSeparator()
                + hubCoreCommands(order);

        return order == 1 ? peerSending : hubCoreCommands(order);
    }

    public String hubB2Commands(int order) {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");

        String peerSending = CommandListToFile.SEND_MESSAGE + "HUB_B2" + " " + "sn/char"
                + hubCoreCommands(2);

        return order == 1 ? peerSending : hubCoreCommands(1);
    }

    /**
     * Generates command list for a peer to disconnect from the hub after sending a message.
     */
    public String hubDisA1Commands(int order) {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");

        String peerSending = hubCoreCommands(order)
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + "HUB_DisA1" + " " + "sn/char";

        String peerReceiving = hubCoreCommands(order)
                + disconnectFromHub();

        return order == 1 ? peerSending : peerReceiving;
    }

    /**
     * Generates command list for a peer to disconnect from the hub after sending a message and the other peer to send a message.
     * @param order in which the peer connects to the hub; 1 or 2
     * @return the command list as a string
     */
    public String hubDisA2Commands(int order) {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");
        String peerSender = hubCoreCommands(order);

        String peerReceiver = hubCoreCommands(order)
                + disconnectFromHub()
                + CommandListToFile.SEND_MESSAGE + "HUB_DisA2" + " " + "sn/char";

        return order == 1 ? peerSender : peerReceiver;
    }

    /**
     * Generates command list for both peers to connect to the hub, one immediately disconnects after and the other sends a message.
     * @param order
     * @return
     */
    public String hubDisBCommands(int order) {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");

        String peerSending = hubCoreCommands(order)
                + disconnectFromHub();

        String peerReceiving = hubCoreCommands(order)
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + "HUB_DisB" + " " + "sn/char";

        return order == 2 ? peerSending : peerReceiving;
    }

}
