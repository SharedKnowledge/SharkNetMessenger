package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.CommandListToFile;

public class CoreScenariosHub {

    public static final String START_HUB = "startHub";
    public static final String IP = "localhost";
    public static final String CONNECT_HUB = "connectHub";
    public static final String DISCONNECT_HUB = "disconnectHub";
    public static final int PORT = 6907;

    public static final String CONNECT_HUB_LINE = CONNECT_HUB + " " + IP + " " + PORT;
    public static final String START_HUB_LINE = START_HUB + " " + IP + " " + PORT;


    public CoreScenariosHub() {
    }

    private String hubHostCommandList() {
        return START_HUB_LINE;
    }

    private String hubCoreAConnectorCommandList(int order) {
        if (order == 1) {
            return CONNECT_HUB_LINE + System.lineSeparator();
        }
        return CommandListToFile.WAIT + " " + 500 + System.lineSeparator()
                + CONNECT_HUB_LINE + System.lineSeparator();
    }

    public String hubACommandList(int index, char peer) {
        if (index == 1) {
            if (peer == 'a' || peer == 'A') {
                return aSendMessageCommands(index);
            } else if (peer == 'b' || peer == 'B') {
                return receiveMessageCommands();
            }
        } else if (index == 2) {
            if (peer == 'a' || peer == 'A') {
                return receiveMessageCommands();
            }
            if (peer == 'b' || peer == 'B') {
                return aSendMessageCommands(index);
            }
        }
        throw new IllegalArgumentException();
    }

    public String hubBCommandList(int index, char peer) {
        if (index == 1) {
            if (peer == 'a' || peer == 'A') {
                return bSendMessageCommands(index);
            } else if (peer == 'b' || peer == 'B') {
                return receiveMessageCommands();
            }
        } else if (index == 2) {
            if (peer == 'a' || peer == 'A') {
                return receiveMessageCommands();
            } else if (peer == 'b' || peer == 'B') {
                return bSendMessageCommands(index);
            }
        }
        throw new IllegalArgumentException();
    }

    private String aSendMessageCommands(int index) {
        return hubCoreAConnectorCommandList(1)
                + CommandListToFile.SEND_MESSAGE + "HUB_A" + index + " " + "sn/char";
    }



    private String bSendMessageCommands(int index) {
        return CommandListToFile.SEND_MESSAGE + "HUB_B" + index + " " + "sn/char"
                + System.lineSeparator()
                + hubCoreAConnectorCommandList(1)
                + CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME;
    }

    private String receiveMessageCommands() {
        return hubCoreAConnectorCommandList(2) +
                CommandListToFile.WAIT + " " + CommandListToFile.WAIT_TIME
                + System.lineSeparator()
                + CommandListToFile.LIST_MESSAGES;
    }
}

    //think AGAIN!!!
//    public void hubCoreScenarioWriter(int order) {
//        if (order > 2 || order < 1) {
//            throw new IllegalArgumentException();
//        }
//        try {
//            if (order == 1) {
//                FileUtils.writeToFile(new FileOutputStream("/hubCore1PeerA.txt"),
//                        hubCoreAConnectorCommandList(1));
//                FileUtils.writeToFile(new FileOutputStream("/hubCore1PeerB.txt"),
//                        hubCoreAConnectorCommandList(2));
//            }
//            else {
//                FileUtils.writeToFile(new FileOutputStream("/hubCore2PeerA.txt"),
//                        hubCoreAConnectorCommandList(2));
//                FileUtils.writeToFile(new FileOutputStream("/hubCore2PeerB.txt"),
//                        hubCoreAConnectorCommandList(1));
//            }
//        } catch (IOException e) {
//            System.err.println(e.getMessage());
//        }
    }
}
