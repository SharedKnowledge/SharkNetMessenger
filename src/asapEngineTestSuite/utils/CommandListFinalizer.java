package asapEngineTestSuite.utils;

/**
 * This class provides a template to finalise a command list for peers in an automated test scenario.
 */
public class CommandListFinalizer {

	private static final String lsMessages = CommandListToFile.LIST_MESSAGES;
	private static final String exit = CommandListToFile.EXIT;
	private static final String CLI_SEPARATOR = ";";


	/**
	 * Finalises the command list by appending standard commands for listing messages and exiting.
	 * @return the complete command list as a string
	 */
	public static String finalizeCommandList(String commands) {
		return commands
                + lsMessages + " " + 2
                +CLI_SEPARATOR
                + lsMessages + " " + 1
                + CLI_SEPARATOR
                + exit
                + CLI_SEPARATOR;
	}

	/**
	 * Finalises the command list by appending standard commands for listing messages, waiting for a specified time, and exiting.
	 * @param waitMs the time in milliseconds to wait before listing messages
	 * @return the complete command list as a string
	 */
	public static String finalizeCommandList(String commands, int waitMs) {
		String wait = CommandListToFile.WAIT;

		return commands
                + wait + " " + waitMs
                + CLI_SEPARATOR
                + lsMessages + " " + 2
                + CLI_SEPARATOR
                + lsMessages + " " + 1
                + CLI_SEPARATOR
                + exit
                + CLI_SEPARATOR;
	}

	/**
	 * Finalises the command list for a hub host by appending commands to stop the hub and exit.
	 * @param commands the initial command list
	 * @return the complete command list as a string
	 */
	public static String finalizeCommandListHubHost(String commands, int waitMs) {
		return commands
                + CommandListToFile.WAIT + " " + waitMs
                + CLI_SEPARATOR
                + "stopHub 6907"
                + CLI_SEPARATOR
                + exit
                + CLI_SEPARATOR;
	}
}
