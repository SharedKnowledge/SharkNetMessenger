package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.CommandListToFile;

public class CoreScenariosTCPChain {

    public static final int PORT_NUMBER = 4444;

    public static final String OPEN_PORT_LINE = CommandListToFile.OPEN_TCP + " " + PORT_NUMBER;

    private String coreARecevingPeer() {
        return OPEN_PORT_LINE
                + System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME * 2
                + System.lineSeparator()
                + CommandListToFile.LIST_MESSAGES;
    }

    private String coreASendingPeer() {
       return CommandListToFile
               .WAIT + " " + CommandListToFile.WAIT_TIME
               + System.lineSeparator()
               + CommandListToFile.CONNECT_TCP + "FILLER_IP" + PORT_NUMBER
               + System.lineSeparator()
               + CommandListToFile.SEND_MESSAGE + " " + "TCPChain_CoreA1" + " sn/char"
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
            throw new IllegalArgumentException("Order must be 1 or 2.");
    }


    private String coreBSendingPeer() {
        return CommandListToFile.SEND_MESSAGE + " " + "TCPChain_CoreB1" + " sn/char"
                +  System.lineSeparator()
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.CONNECT_TCP + " FILLER_IP " + PORT_NUMBER
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


}