package asapEngineTestSuite.testScenarios.core.hub;
import asapEngineTestSuite.utils.CommandListToFile;

import static asapEngineTestSuite.testScenarios.core.basic.CoreBasicEncounter.*;

public class CoreScenariosHub {

    public static final String START_HUB = "startHub";
    public static final String CONNECT_HUB = "connectHub";
    public static final String DISCONNECT_HUB = "disconnectHub";
    public static final String DISCONNECT_HUB_LINE = DISCONNECT_HUB + " 1";
    private int hubPort = 6907;
    public final String STOP_HUB_LINE = "stopHub " + hubPort;

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
        return START_HUB + " " + hubPort + CLI_SEPARATOR;
    }

    /**
     * Generates command lists for HubTX_Length with {@code x} Peers and a File of {@code length} Bytes.
     * @param x the total peer count including the peer which starts the hub
     * @param length the length of the message to be sent
     * @return The command lists
     */
    public String[] hubTX_Length(int x, int length) {
        String[] commands = new String[x];

        commands[0] = hubHostCommand()
                + CONNECT_HUB + " FILLER_IP " + hubPort
                + CLI_SEPARATOR;
        // Give receiver peers enough time to connect and reach their initial block commands.
        commands[0] += CommandListToFile.WAIT + " 3000"
            + CLI_SEPARATOR;
        for (int i = 1; i < x; i++) {
            commands[0] += CLI_RELEASE + " P" + i
                    + CLI_SEPARATOR
                    + WAIT + " " + (1000)
                    + CLI_SEPARATOR;
        }
        for (int i = 1; i < x; i++) {
            commands[0] += CLI_BLOCK + " P_hub" + i
                    + CLI_SEPARATOR;
        }
        commands[0] += CommandListToFile.WAIT + " 1000"
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + " HubT" + x + length + ".txt" + " sn/file"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000 * x)
                + CLI_SEPARATOR
                + STOP_HUB_LINE
                + CLI_SEPARATOR;
        for (int i = 1; i < x; i++) {
            commands[i] = CommandListToFile.WAIT + " " + (1000)
                    + CLI_SEPARATOR
                    + CONNECT_HUB + " FILLER_IP " + hubPort
                    + CLI_SEPARATOR
                    + CommandListToFile.WAIT + " " + (1000)
                    + CLI_SEPARATOR
                    + CLI_BLOCK + " P" + i
                    + CLI_SEPARATOR
                    + CLI_RELEASE + " P_hub" + i
                    + CLI_SEPARATOR
                    + CommandListToFile.WAIT + " " + (1000 * x)
                    + CLI_SEPARATOR;
        }
        return commands;
    }

    /**
     * Generates command list for HubTStalling with 2 Peers and a File of {@code length} Bytes.
     * @param length the length of the message to be sent
     * @return the command lists
     */
    public String[] hubTStalling_Length(int length) {

        String[] commands = new String[2];

        commands[0] = hubHostCommand()
                + CONNECT_HUB + " FILLER_IP " + hubPort
                + CLI_SEPARATOR
                + CLI_BLOCK + " P_hub"
                + CLI_SEPARATOR
                + WAIT + " " + (1000)
                + CLI_SEPARATOR
                + CLI_RELEASE + " P2"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " 1000"
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + " HubTStalling" + length + "_1.txt" + " sn/file"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR
                +  CommandListToFile.SEND_MESSAGE + " HubTStalling" + length + "_2.txt" + " sn/file"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + " HubTStalling" + length + "_3.txt" + " sn/file"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR
                + CLI_RELEASE + " P3"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR
                + STOP_HUB_LINE
                + CLI_SEPARATOR;

        commands[1] = CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR
                + CONNECT_HUB + " FILLER_IP " + hubPort
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR
                + CLI_RELEASE + " P_hub"
                + CLI_SEPARATOR
                + CLI_BLOCK + " P2"
                + CLI_SEPARATOR
                + CLI_BLOCK + " P3"
                + CLI_SEPARATOR
                + CommandListToFile.WAIT + " " + (1000)
                + CLI_SEPARATOR;

        return commands;
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
    @Deprecated
    public String[] hubA1Commands() throws IllegalArgumentException {
        String[] commands = new String[2];
        commands[0] = hubCoreCommands(1)
                    + CommandListToFile.SEND_MESSAGE + " HUBCoreA1_" + " " + "sn/characters"
                    + System.lineSeparator();
        commands[1] = hubCoreCommands(2);
        return commands;
    }

    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
