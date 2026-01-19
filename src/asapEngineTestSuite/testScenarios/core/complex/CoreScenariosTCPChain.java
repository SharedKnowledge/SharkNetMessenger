package asapEngineTestSuite.testScenarios.core.complex;

import asapEngineTestSuite.CoreScenarioOutput;
import asapEngineTestSuite.testScenarios.core.basic.CoreBasicEncounter;
import asapEngineTestSuite.utils.CommandListToFile;


/**
 * Each peer connects to the previous peer in order.
 */
public class CoreScenariosTCPChain extends CoreBasicEncounter {


    int portNr = 4444;

    private final String openPortLine = CommandListToFile.OPEN_TCP + " " + portNr;
    public static final String FILLER_IP = " FILLER_IP ";
    public static final String SN_CHARACTERS = " sn/characters";
    public static final String CLOSE_ENCOUNTER_1 = CommandListToFile.CLOSE_ENCOUNTER + " 1" + CLI_SEPARATOR;
    public static final int WAIT_TIME = 1000;
    public static final String WAIT = CLI_WAIT;
    public static final String RELEASE = CLI_RELEASE;
    public static final String BLOCK = CLI_BLOCK;

    /**
     *
     * @param peerIndex
     * @return
     */
    public String syncMarkerGenerator(int peerIndex) {
        return " P" + peerIndex;
    }

    private String coreADisSendingPeer(int peerOrder) {
        if (peerOrder != 1 && peerOrder != 2)
            throw new IllegalArgumentException();
        return WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
//              + CommandListToFile.SEND_MESSAGE + " " + CoreScenarioOutput.tcpChainCore("CoreA") + peerOrder + "_Dis " +  SN_CHARACTERS
                +  CLI_SEPARATOR;
    }

    public String[] coreADisCommands(int peerOrder, String syncDescriptor) throws IllegalArgumentException {
        validation(syncDescriptor);
        String[] commandLists = new String[2];
        if (peerOrder == 1) {
            commandLists[0] = coreADisSendingPeer(1);
            commandLists[1] = csRecevingPeer(syncDescriptor);
        } else {
            commandLists[0] = csRecevingPeer(syncDescriptor);
            commandLists[1] = coreADisSendingPeer(2);
        }
        return commandLists;
    }

    private String coreBDisSendingPeer(int peerCount) {
        return  WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + " " + CoreScenarioOutput.tcpChainCore("CoreB") + peerCount + "_Dis " + SN_CHARACTERS
                +  CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + CLI_SEPARATOR;
    }

    public String[] coreBDisCommands(int peerOrder, String syncDescriptor) {
        validation(syncDescriptor);
        String[] commandLists = new String[2];
        if (peerOrder == 1) {
            commandLists[0] = coreBDisSendingPeer(1);
            commandLists[1] = scReceivingPeer(syncDescriptor);
        } else {
            commandLists[0] = scReceivingPeer(syncDescriptor);
            commandLists[1] = coreBDisSendingPeer(2);
        }
        return commandLists;
    }

//    public String[] coreADisCommandLists(int peerOrder, String syncDescriptor) {
////        validation(peerOrder);
//        String[] commandLists = new String[2];
//
//        if (peerOrder == 1) {
//            commandLists[0] = coreADisCommands(1, syncDescriptor)[0]
//                    + CLOSE_ENCOUNTER_1
//                    + WAIT + " " + 3000
//                    + CLI_SEPARATOR//                    + coreADisSendingPeer(1);
//            commandLists[1] = coreADisCommands(1, syncDescriptor)[1];
//        } else {
//            commandLists[0] = coreADisCommands(2, syncDescriptor)[0];
//            commandLists[1] = coreADisCommands(2, syncDescriptor)[1]
//                    + CLOSE_ENCOUNTER_1
//                    + WAIT + " " + 3000
//                    + CLI_SEPARATOR//                    + coreADisSendingPeer(2);
//        }
//        return commandLists;
//    }

    public String[] chainXCommands(String syncMarker, int peerCount) throws IllegalArgumentException {
        validation(syncMarker);
        int testDauer = WAIT_TIME * (peerCount);

        String[] commandLists = new String[peerCount];

        commandLists[0] = openPortLine
                + CLI_SEPARATOR
                + "echo openTCP"
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + FILLER_FILENAME + SN_FILE
                + CLI_SEPARATOR
                + "echo sendMessage"
                + CLI_SEPARATOR
                + BLOCK + syncMarkerGenerator(1)
                + CLI_SEPARATOR
                + WAIT + " " + testDauer/(peerCount*2)
                + CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME * (2 * peerCount)
                + CLI_SEPARATOR;

        for (int i = 1; i < peerCount; i++) {
            if (i != peerCount - 1) {
                commandLists[i] = "openTCP" + " " + (portNr + i)
                        + CLI_SEPARATOR
                        + "echo openTCP"
                        + CLI_SEPARATOR
                        + WAIT + " " + WAIT_TIME * i
                        + CLI_SEPARATOR;
            } else {
                commandLists[i] = WAIT + " " + testDauer
                        + CLI_SEPARATOR;
            }
            commandLists[i] += CommandListToFile.CONNECT_TCP + FILLER_IP + (portNr + i - 1)
                    + CLI_SEPARATOR
                    + WAIT + " " + WAIT_TIME * i
                    + CLI_SEPARATOR
                    + "echo connectTCP"
                    + CLI_SEPARATOR
                    + RELEASE + syncMarkerGenerator(i)
                    + CLI_SEPARATOR;
            if (i != peerCount - 1) {
                        commandLists[i] += BLOCK + syncMarkerGenerator(i + 1)
                        + CLI_SEPARATOR;
            }
            commandLists[i]+= WAIT + " " + WAIT_TIME * i
                    + CLI_SEPARATOR;
        }
        return commandLists;
    }

    public String[] chainLTXcommands(String syncMarker, int peerCount) throws IllegalArgumentException {
        validation(syncMarker);
        // Sa:1;Cab;Dab;Cbc;Dbc;Ccd;Dcd;Eb:1;Ec:1;Ed:1
        int testDauer = WAIT_TIME * (peerCount);

        String[] commandLists = new String[peerCount];

        commandLists[0] = CommandListToFile.SEND_MESSAGE + FILLER_FILENAME + SN_FILE
                + CLI_SEPARATOR
                + "echo sendMessage"
                + CLI_SEPARATOR
                + openPortLine
                + CLI_SEPARATOR
                + "echo openTCP"
                + CLI_SEPARATOR
                + BLOCK + syncMarkerGenerator(1)
                + CLI_SEPARATOR
                + "closeEncounter 1"
                + CLI_SEPARATOR
                + "echo closeEncounter"
                + CLI_SEPARATOR
                + "exit";

        for (int i = 1; i <= peerCount; i++) {
            commandLists[i] = WAIT  + " " + (testDauer / peerCount) * i
                    + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                    + CLI_SEPARATOR
                    + "echo connectTCP"
                    + CLI_SEPARATOR
                    + RELEASE + syncMarkerGenerator(i)
                    + CLI_SEPARATOR
                    + CommandListToFile.LIST_MESSAGES
                    + CLI_SEPARATOR

        }

        //C

        //D
        return commandLists;
    }
}