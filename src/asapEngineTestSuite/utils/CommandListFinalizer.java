package asapEngineTestSuite.utils;

/**
 * This class provides a template to finalise a command list for peers in an automated test scenario.
 */
public class CommandListFinalizer {

	private static final String lsMessages = CommandListToFile.LIST_MESSAGES;
	private static final String exit = CommandListToFile.EXIT;


	/**
	 * Finalises the command list by appending standard commands for listing messages and exiting.
	 * @return the complete command list as a string
	 */
	public static String finalizeCommandList(String commands) {
		return commands
			+ System.lineSeparator()
			+ lsMessages
			+ System.lineSeparator()
			+ exit;
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
			+ System.lineSeparator()
			+ lsMessages
			+ System.lineSeparator()
			+ exit;
	}
}
