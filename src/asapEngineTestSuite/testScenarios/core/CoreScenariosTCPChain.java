package asapEngineTestSuite.testScenarios.core;

import asapEngineTestSuite.utils.CommandListToFile;

public class CoreScenariosTCPChain {


    private int randomPortGen() {
        int min = 4000;
        int max = 9900;
	    return min + (int) (Math.random() * ((max - min) + 1));
    }

    int randomPort = randomPortGen();

    private final String openPortLine = CommandListToFile.OPEN_TCP + " " + randomPort;
    public static final String FILLER_IP = " FILLER_IP ";
    public static final String TCPCHAIN_CORE_A = "TCPChain_CoreA";
    public static final String TCPCHAIN_CORE_B = "TCPChain_CoreB";
    public static final String SN_CHARACTERS = " sn/characters";
    public static final String CLOSE_ENCOUNTER_1 = CommandListToFile.CLOSE_ENCOUNTER + " 1" + System.lineSeparator();

    private String coreARecevingPeer() {
        return openPortLine
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 4
                + System.lineSeparator();
    }


    private String coreASendingPeer(int peerOrder) {
        if (peerOrder != 1 && peerOrder != 2)
            throw new IllegalArgumentException();
       return CommandListToFile
               .WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.CONNECT_TCP + FILLER_IP + randomPort
               + System.lineSeparator()
               + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                 + System.lineSeparator()
               + CommandListToFile.SEND_MESSAGE + " " + TCPCHAIN_CORE_A + peerOrder + SN_CHARACTERS
               +  System.lineSeparator();
    }

    public String[] coreACommandLists(int peerOrder) throws IllegalArgumentException {
        validation(peerOrder);
            String[] commandLists = new String[2];
            if (peerOrder == 1) {
                commandLists[0] = coreASendingPeer(1);
                commandLists[1] = coreARecevingPeer();
        } else {
                commandLists[0] = coreARecevingPeer();
                commandLists[1] = coreASendingPeer(2);
        }
        return commandLists;
    }

    private static void validation(int peerOrder) {
        if (peerOrder < 1 || peerOrder > 2)
            throw new IllegalArgumentException();
    }


    private String coreBSendingPeer(int peerOrder) {
        if (peerOrder != 1 && peerOrder != 2)
            throw new IllegalArgumentException();
        return CommandListToFile.SEND_MESSAGE + " " + TCPCHAIN_CORE_B + peerOrder + SN_CHARACTERS
                +  System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + FILLER_IP + randomPort
                + System.lineSeparator();
    }

    private String coreBReceivingPeer() {
        return openPortLine
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 5
                + System.lineSeparator();
    }

    /**
     *
     * @param peerOrder the order of peers in which they establish the connection. (1: Peer A connects first, 2: B connects first)
     * @return the command lists required for the Scenario Core B.
     */
    public String[] coreBCommandLists(int peerOrder) {
        validation(peerOrder);
        String[] commandLists = new String[2];
        if (peerOrder == 1) {
                commandLists[0] = coreBSendingPeer(1);
                commandLists[1] = coreBReceivingPeer();
        } else {
                commandLists[0] = coreBReceivingPeer();
                commandLists[1] = coreBSendingPeer(2);
        }
        return commandLists;
    }

    public String[] coreADisCommandLists(int peerOrder) {
        validation(peerOrder);
        String[] commandLists = new String[2];

        if (peerOrder == 1) {
            commandLists[0] = coreACommandLists(1)[0]
                    + CLOSE_ENCOUNTER_1
                    + CommandListToFile.WAIT + " " + 500
                    + coreASendingPeer(1);
            commandLists[1] = coreACommandLists(1)[1];
        } else {
            commandLists[0] = coreACommandLists(2)[0];
            commandLists[1] = coreACommandLists(2)[1]
                    + CLOSE_ENCOUNTER_1
                    + CommandListToFile.WAIT + " " + 500
                    + coreASendingPeer(2);
        }
        return commandLists;
    }

    public String[] coreBDisCommandLists(int peerOrder) {
        validation(peerOrder);

        String[] commandLists = new String[2];

        if (peerOrder == 1) {
            commandLists[0] = coreBCommandLists(1)[0]
                    + CLOSE_ENCOUNTER_1
                    + coreBSendingPeer(1);
            commandLists[1] = coreBCommandLists(1)[1];
        } else {
            commandLists[0] = coreBCommandLists(2)[0];
            commandLists[1] = coreBCommandLists(2)[1]
                    + CLOSE_ENCOUNTER_1
                    + coreBSendingPeer(2);
        }
        return commandLists;
    }

    /**
     * Adds the closeEncounter command to one of the command lists.
     * @param commandLists the command lists to execute the scenario.
     * @param peer A or B (case-insensitive)
     * @return the array with the modified command lists.
     */
    public static String[] appendCommandListWithCloseEncounter(String[] commandLists, char peer) throws NullPointerException, IllegalArgumentException {
        if (commandLists == null)
            throw new NullPointerException();
        if (peer != 'a' && peer != 'A' && peer != 'b' && peer != 'B')
            throw new IllegalArgumentException("Peer must be 'A' or 'B' (case-insensitive)");
        if (peer == 'a' || peer == 'A') {
            commandLists[0] += CLOSE_ENCOUNTER_1;
        }
        if (peer == 'b' || peer == 'B') {
            commandLists[1] += CLOSE_ENCOUNTER_1;
        }
        return commandLists;
    }



}