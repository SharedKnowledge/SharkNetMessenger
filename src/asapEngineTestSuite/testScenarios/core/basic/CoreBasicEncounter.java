package asapEngineTestSuite.testScenarios.core.basic;

import asapEngineTestSuite.utils.CommandListToFile;

/**
 * Basic encounter command list generation
 */
public class CoreBasicEncounter {
    protected int portNr = 4444;

    /**
     *
     */
    public final String openPortLine = CommandListToFile.OPEN_TCP + " " + portNr;
    public static final String FILLER_IP = " FILLER_IP ";
    public static final String SN_CHARACTERS = " sn/characters";
    public static final String SN_FILE = "sn/file";
    // Local CLI constants (previously from TestLanguageCompiler)
    public static final String CLI_SEPARATOR = ";";
    public static final String CLI_WAIT = "wait";
    public static final String CLI_RELEASE = "release";
    public static final String CLI_BLOCK = "block";

    public static final String CLOSE_ENCOUNTER_1 = CommandListToFile.CLOSE_ENCOUNTER + " 1" + CLI_SEPARATOR;
    public static final int WAIT_TIME = 500;
    public static final String WAIT = CLI_WAIT;
    public static final String RELEASE = CLI_RELEASE;
    public static final String BLOCK = CLI_BLOCK;
    public static final String FILLER_FILENAME = " FILLER_FILENAME ";

    /**
     * A method to validate input parameters
     * @param syncMarker the synchronization marker
     * @throws IllegalArgumentException if syncMarker is null or empty
     */
    public static void validation(String syncMarker) throws IllegalArgumentException {
        if (syncMarker == null ||  syncMarker.isEmpty()) {
            throw  new IllegalArgumentException("syncMarker is null or empty");
        }
    }

    /**
     * Method to generate CS command list for receiving peer
     * @param syncMarker the synchronization marker
     * @return the command list string
     * @throws IllegalArgumentException if syncMarker is null or empty
     */
    public String csRecevingPeer(String syncMarker) throws IllegalArgumentException {
        validation(syncMarker);
        return openPortLine
                + CLI_SEPARATOR
                + BLOCK + " " + syncMarker
                + CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME * 2
                + CLI_SEPARATOR;
    }

    /**
     * Method to generate CS command list for sending peer
     * @param syncDescriptor the synchronization descriptor
     * @return the command list string
     * @throws IllegalArgumentException if syncDescriptor is null or empty
     */
    public String csSendingPeer(String syncDescriptor) throws IllegalArgumentException {
        validation(syncDescriptor);
        return WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + RELEASE + " " + syncDescriptor
                + CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + FILLER_FILENAME + SN_FILE
                +  CLI_SEPARATOR;
    }

    /**
     * Method to generate SC command list for sending peer
     * @param syncDescriptor the synchronization descriptor
     * @return the command list string
     * @throws IllegalArgumentException if syncDescriptor is null or empty
     */
    public String scSendingPeer(String syncDescriptor) throws IllegalArgumentException {
        validation(syncDescriptor);
        return  WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.SEND_MESSAGE + FILLER_FILENAME + SN_FILE + " " + 1
                +  CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR
                + CommandListToFile.CONNECT_TCP + FILLER_IP + portNr
                + CLI_SEPARATOR
                + RELEASE + " " + syncDescriptor
                + CLI_SEPARATOR;
    }

    /**
     * Method to generate SC command list for receiving peer
     * @param syncMarker the synchronization marker
     * @return the command list string
     * @throws IllegalArgumentException if syncMarker is null or empty
     */
    public String scReceivingPeer(String syncMarker) throws IllegalArgumentException{
        validation(syncMarker);
        return openPortLine
                + CLI_SEPARATOR
                + BLOCK + " " + syncMarker
                + CLI_SEPARATOR
                + WAIT + " " + WAIT_TIME
                + CLI_SEPARATOR;
    }

    /**
     * Method to generate CS command lists for sending and receiving peers
     * @param syncMarker the synchronization marker
     * @return an array of command lists: [0] - sending peer, [1] - receiving peer
     * @throws IllegalArgumentException if syncMarker is null or empty
     */
    public String[] csCommandLists(String syncMarker) throws IllegalArgumentException {
        validation(syncMarker);
        String[] commandLists = new String[2];
        commandLists[0] = csSendingPeer(syncMarker);
        commandLists[1] = csRecevingPeer(syncMarker);
        return commandLists;
    }

    /**
     * Method to generate SC command lists for sending and receiving peers
     * @param syncMarker the synchronization marker
     * @return an array of command lists: [0] - sending peer, [1] - receiving peer
     * @throws IllegalArgumentException if syncMarker is null or empty
     */
    public String[] scCommandLists(String syncMarker) throws IllegalArgumentException {
        validation(syncMarker);
        String[] commandLists = new String[2];
        commandLists[0] = scSendingPeer(syncMarker);
        commandLists[1] = scReceivingPeer(syncMarker);
        return commandLists;
    }
}
