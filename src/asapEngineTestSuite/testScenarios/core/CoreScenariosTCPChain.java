package asapEngineTestSuite.testScenarios.core;

import asapEngineTestSuite.CoreScenarioOutput;
import asapEngineTestSuite.utils.CommandListToFile;

@Deprecated
public class CoreScenariosTCPChain {


    int portNr = 4444;

    private final String openPortLine = CommandListToFile.OPEN_TCP + " " + portNr;
    public static final String FILLER_IP = " FILLER_IP ";
    public static final String SN_CHARACTERS = " sn/characters";
    public static final String CLOSE_ENCOUNTER_1 = CommandListToFile.CLOSE_ENCOUNTER + " 1" + System.lineSeparator();

    private String coreARecevingPeer() {
        return openPortLine
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 6
                + System.lineSeparator();
    }


    private String coreASendingPeer(int peerOrder) {
        if (peerOrder != 1 && peerOrder != 2)
            throw new IllegalArgumentException();
       return CommandListToFile
               .WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
               + System.lineSeparator()
               + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                 + System.lineSeparator()
               + CommandListToFile.SEND_MESSAGE + " " + CoreScenarioOutput.tcpChainCore("CoreA") + peerOrder + SN_CHARACTERS
               +  System.lineSeparator();
    }

    private String coreADisSendingPeer(int peerOrder) {
        if (peerOrder != 1 && peerOrder != 2)
            throw new IllegalArgumentException();
        return CommandListToFile
                .WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + " " + CoreScenarioOutput.tcpChainCore("CoreA") + peerOrder + "_Dis " +  SN_CHARACTERS
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

    public String[] coreADisCommands(int peerOrder) throws IllegalArgumentException {
        validation(peerOrder);
        String[] commandLists = new String[2];
        if (peerOrder == 1) {
            commandLists[0] = coreADisSendingPeer(1);
            commandLists[1] = coreARecevingPeer();
        } else {
            commandLists[0] = coreARecevingPeer();
            commandLists[1] = coreADisSendingPeer(2);
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
        return  CommandListToFile
                .WAIT + " " + CommandListToFile.WAIT_TIME / 3
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + " " + CoreScenarioOutput.tcpChainCore("CoreB") + peerOrder + SN_CHARACTERS
                +  System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + System.lineSeparator();
    }

    private String coreBDisSendingPeer(int peerOrder) {
        if (peerOrder != 1 && peerOrder != 2)
            throw new IllegalArgumentException();
        return  CommandListToFile
                .WAIT + " " + CommandListToFile.WAIT_TIME / 3
                + System.lineSeparator()
                + CommandListToFile.SEND_MESSAGE + " " + CoreScenarioOutput.tcpChainCore("CoreB") + peerOrder + "_Dis " + SN_CHARACTERS
                +  System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + System.lineSeparator();
    }

    private String coreBReceivingPeer() {
        return openPortLine
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 6
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

    public String[] coreBDisCommands(int peerOrder) {
        validation(peerOrder);
        String[] commandLists = new String[2];
        if (peerOrder == 1) {
            commandLists[0] = coreBDisSendingPeer(1);
            commandLists[1] = coreBReceivingPeer();
        } else {
            commandLists[0] = coreBReceivingPeer();
            commandLists[1] = coreBDisSendingPeer(2);
        }
        return commandLists;
    }

    public String[] coreADisCommandLists(int peerOrder) {
        validation(peerOrder);
        String[] commandLists = new String[2];

        if (peerOrder == 1) {
            commandLists[0] = coreADisCommands(1)[0]
                    + CLOSE_ENCOUNTER_1
                    + CommandListToFile.WAIT + " " + 3000
                    + System.lineSeparator()
                    + coreADisSendingPeer(1);
            commandLists[1] = coreADisCommands(1)[1];
        } else {
            commandLists[0] = coreADisCommands(2)[0];
            commandLists[1] = coreADisCommands(2)[1]
                    + CLOSE_ENCOUNTER_1
                    + CommandListToFile.WAIT + " " + 3000
                    + System.lineSeparator()
                    + coreADisSendingPeer(2);
        }
        return commandLists;
    }

    public String[] coreBDisCommandLists(int peerOrder) {
        validation(peerOrder);

        String[] commandLists = new String[2];

        if (peerOrder == 1) {
            commandLists[0] = coreBDisCommands(1)[0]
                    + CLOSE_ENCOUNTER_1
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                    + System.lineSeparator()
                    + coreBSendingPeer(1)
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                    + System.lineSeparator();
            commandLists[1] = coreBDisCommands(1)[1];
        } else {
            commandLists[0] = coreBDisCommands(2)[0];
            commandLists[1] = coreBDisCommands(2)[1]
                    + CLOSE_ENCOUNTER_1
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                    + System.lineSeparator()
                    + coreBSendingPeer(2)
                    + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                    + System.lineSeparator();
        }
        return commandLists;
    }

    /**
     * Adds the closeEncounter command to one of the command lists.
     * @param commandLists the command lists to execute the scenario.
     * @param peer A or B (case-insensitive)
     * @return the array with the modified command lists.
     */
    @Deprecated
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