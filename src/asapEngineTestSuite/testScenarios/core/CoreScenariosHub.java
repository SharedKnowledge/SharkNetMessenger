package asapEngineTestSuite.testScenarios.core;
import asapEngineTestSuite.utils.CommandListToFile;

public class CoreScenariosHub {

    public static final String START_HUB = "startHub";
    public static final String CONNECT_HUB = "connectHub";
    public static final String DISCONNECT_HUB = "disconnectHub";
    public static final String DISCONNECT_HUB_LINE = DISCONNECT_HUB + " 1";
    private int hubPort = 6907;

    public CoreScenariosHub() {}


    /**
     * Constructor for CoreScenariosHub.
     */
    public CoreScenariosHub(int hubStartingPort) {
        this.hubPort = hubStartingPort;
    }

    private static void validateOrder(int order) throws IllegalArgumentException {
        if (order > 2 || order < 1)
            throw new IllegalArgumentException("Order must be 1 or 2.");
    }

    /**
     * Generates command list for starting the hub.
     * @return the command list as a string
     */
    public final String hubHostCommand() {
        return START_HUB + " " + hubPort + System.lineSeparator();
    }

    /**
     * Generates command list for a peer to connect to the hub before the other peer.
     * @return the command list as a string
     */
    public String hubCoreCommands(int order) throws IllegalArgumentException {
        validateOrder(order);

        String peerPrimary = CommandListToFile.WAIT + " " + 2000
                + System.lineSeparator() // DO NOT TOUCH
                + CONNECT_HUB + " " + "FILLER_IP" + " " + hubPort
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + 2000
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + 1000
                + System.lineSeparator()
                + "lsEncounter" // hard coded
                + System.lineSeparator();

        if (order == 1) {
            return peerPrimary;
        }
        else {
                return CommandListToFile.WAIT + " " + 500
                        + System.lineSeparator()
                        + peerPrimary
                        + CommandListToFile.WAIT + " " + 500
                        + System.lineSeparator();
            }
    }

    private String disconnectFromHub() {
        return DISCONNECT_HUB_LINE + System.lineSeparator();
    }

    /**
     * Generates command lists for the scenario in which the peers connect to the hub and one sends a message.
     * Here, the peer that connects first is the one that sends the message.
     * @return the command list for the peer in this order as a string
     * @throws IllegalArgumentException if the order is not 1 or 2
     */
    public String[] hubA1Commands() throws IllegalArgumentException {
        String[] commands = new String[2];
        commands[0] = hubCoreCommands(1)
                    + CommandListToFile.SEND_MESSAGE + " HUBCoreA1_" + " " + "sn/characters"
                    + System.lineSeparator();
        commands[1] = hubCoreCommands(2);
        return commands;
    }

    public String[] hubA2Commands() throws IllegalArgumentException {
        String[] commands = new String[2];
        commands[0] = hubCoreCommands(2)
                    + CommandListToFile.SEND_MESSAGE + " HUBCoreA2_" + " " + "sn/characters"
        + System.lineSeparator();
        commands[1] = hubCoreCommands(1);
        return commands;
    }

    /**
     * Generates command list for the scenario where one of the peers sends a message and then peers connect.
     * Here, the peer that sends the message is the one that connects first.
     * @return the command list for the peer in this order as a string
     * @throws IllegalArgumentException if the order is not 1 or 2
     */
    public String[] hubB1Commands() throws IllegalArgumentException {
        String[] commands = new String[2];
		commands[0] = CommandListToFile.SEND_MESSAGE + " HUBCoreB1_" + " " + "sn/characters"
		    + System.lineSeparator()
		    + hubCoreCommands(1);
       commands[1] = hubCoreCommands(2);
       return commands;
    }

    /**
     * Generates command list for a peer to connect to the hub after the other peer and send a message.
     * The peer that connects second sends a message.
     * @return the command list as a string
     */
    public String[] hubB2Commands() {
        String[] commands = new String[2];
        commands[0] = CommandListToFile.SEND_MESSAGE + " HUBCoreB2_" + " " + "sn/characters"
                + System.lineSeparator()
                + hubCoreCommands(2);

        commands[1] = hubCoreCommands(1);
        return commands;
    }

    /**
     * Generates command lists for the scenario where both peers connect to the hub, one sends a message,
     * while the other immediately disconnects.
     */
    public String[] hubDisA1Commands() {
        String[] commands = new String[2];
        commands[0] = hubCoreCommands(1)
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + " HUBCoreA1_Dis_" + " " + "sn/characters"
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 4
                + System.lineSeparator();

        commands[1] = hubCoreCommands(2)
                + disconnectFromHub()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 4
                + System.lineSeparator();
        return commands;
    }

    /**
     * Generates command list for a peer to disconnect from the hub after sending a message and the other peer to send a message.
     * @return the command list as a string
     */
    public String[] hubDisA2Commands() throws IllegalArgumentException {
        String[] commands = new String[2];
            //PeerA
		commands[0] = hubCoreCommands(1)
                        + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                        + System.lineSeparator();
            //PeerB
            commands[1] = hubCoreCommands(2)
                    + disconnectFromHub()
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                    + System.lineSeparator()
                    + CommandListToFile.SEND_MESSAGE + " HUBCoreA2_Dis_" + " " + "sn/characters"
                    + System.lineSeparator();
        return commands;
    }

    /**
     * Generates command list for both peers to connect to the hub, one immediately disconnects after and the other sends a message.
     * In this scenario, the peer that connects first is the one that sends the message.
     * @return the command list as a string
     */
    public String[] hubDisB1Commands() {
        String[] commands = new String[2];

        commands[0] = hubCoreCommands(2)
                + disconnectFromHub()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                + System.lineSeparator();

        commands[1] = hubCoreCommands(1)
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + " HUBCoreB1_Dis_" + " sn/characters"
                + System.lineSeparator();
        return commands;
    }

    /**
     * @return the command list as a string array
     */
    public String[] hubDisB2Commands() {
        String[] commands = new String[2];
            commands[0] = hubCoreCommands(1)
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                    + System.lineSeparator()
                    + DISCONNECT_HUB_LINE + System.lineSeparator()
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME / 2
                    + System.lineSeparator()
                    + CommandListToFile.SEND_MESSAGE + " HUBCoreB2_Dis" + " " + "sn/characters"
                    + System.lineSeparator();
            commands[1] = hubCoreCommands(2)
                    + disconnectFromHub()
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                    + System.lineSeparator();
        return commands;
    }
}
