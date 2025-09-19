package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.CommandListToFile;

public class CoreScenariosTCPChain {
    public static final int PORT_NUMBER = 4444;

    public static final String OPEN_PORT_LINE = CommandListToFile.OPEN_TCP + " " + PORT_NUMBER;
    public static final String FILLER_IP = " FILLER_IP ";
    public static final String TCPCHAIN_CORE_A_1 = "TCPChain_CoreA1";
    public static final String TCPCHAIN_CORE_B_1 = "TCPChain_CoreB1";
    public static final String SN_CHAR = " sn/char";

    private String coreARecevingPeer() {
        return OPEN_PORT_LINE
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                + System.lineSeparator()
                + CommandListToFile.LIST_MESSAGES
                + System.lineSeparator();
    }

    private String coreASendingPeer() {
       return CommandListToFile
               .WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.CONNECT_TCP + FILLER_IP + PORT_NUMBER
               + System.lineSeparator()
               + CommandListToFile.SEND_MESSAGE + " " + TCPCHAIN_CORE_A_1 + SN_CHAR
               +  System.lineSeparator()
               + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.LIST_MESSAGES
               + System.lineSeparator();
    }

    public String[] coreACommandLists(int peerOrder) throws IllegalArgumentException {
        validation(peerOrder);
            String[] commandLists = new String[2];
            if (peerOrder == 1) {
                commandLists[0] = coreASendingPeer();
                commandLists[1] = coreARecevingPeer();
        } else {
                commandLists[0] = coreARecevingPeer();
                commandLists[1] = coreASendingPeer();
        }
        return commandLists;
    }

    private static void validation(int peerOrder) {
        if (peerOrder < 1 || peerOrder > 2)
            throw new IllegalArgumentException();
    }


    private String coreBSendingPeer() {
        return CommandListToFile.SEND_MESSAGE + " " + TCPCHAIN_CORE_B_1 + SN_CHAR
                +  System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + FILLER_IP + PORT_NUMBER
                + System.lineSeparator();
    }

    private String coreBReceivingPeer() {
        return OPEN_PORT_LINE
                + System.lineSeparator();
    }

    public String[] coreBCommandLists(int peerOrder) {
        validation(peerOrder);
        String[] commandLists = new String[2];
        if (peerOrder == 1) {
                commandLists[0] = coreBSendingPeer();
                commandLists[1] = coreBReceivingPeer();
        } else {
                commandLists[0] = coreBReceivingPeer();
                commandLists[1] = coreBSendingPeer();
        }
        return commandLists;
    }

    //TODO
    public String[] coreADisconnectSCommandLists() {
            String[] commandLists = new String[2];

            return commandLists;
    }


}