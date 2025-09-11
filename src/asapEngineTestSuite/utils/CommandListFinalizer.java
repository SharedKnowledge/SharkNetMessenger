package asapEngineTestSuite.utils;

/**
 * This class provides a template to finalise a command list for peers in an automated test scenario.
 */
public class CommandListFinalizer {

	private final String commands;
	private static final String lsMessages = CommandListToFile.LIST_MESSAGES;
	private static final String exit = CommandListToFile.EXIT;



	/**
	 * Constructor that initializes the CommandListTemplate with the given commands.
	 * @param commands the initial commands to be included in the command list
	 */
	public CommandListFinalizer(String commands) {
		this.commands = commands;
	}

	/**
	 * Finalises the command list by appending standard commands for listing messages and exiting.
	 * @return the complete command list as a string
	 */
	public String finalizeCommandList() {
		return this.commands
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
	public String finalizeCommandList(int waitMs) {
		String wait = CommandListToFile.WAIT;

		return this.commands
			+ System.lineSeparator()
			+ wait + " " + waitMs
			+ System.lineSeparator()
			+ lsMessages
			+ System.lineSeparator()
			+ exit;
	}
}
