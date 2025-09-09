package asapEngineTestSuite.utils;

/**
 * This class is responsible for generating the command list for the host to execute the test scenario.
 * It creates a command list based on the number of peers and writes it to a file.
 *
 * @author cemreozen
 */
public class TestHost {

	/**
	 * Generates a command list for the host to execute the test scenario.
	 *
	 * @param cltf the utils.CommandListToFile object containing scenario parameters
	 * @return the command list as a string
	 */
	public String hostCommandList(CommandListToFile cltf) {
		int peerCount = cltf.getPeerCount();
		StringBuilder commandList = new StringBuilder();
		for (int i = 1; i <= peerCount; i++) {
			commandList.append(CommandListToFile.SEND_MESSAGE + ' ' + CommandListToFile.SCENARIO_FILE_NAME)
				.append(i)
				.append(CommandListToFile.TEXT_FILE_EXTENSION + ' ')
				.append(CommandListToFile.FORMAT_DESC_FILE)
				.append(System.lineSeparator());
		}
		commandList.append(CommandListToFile.OPEN_TCP + ' ' + CommandListToFile.HOST_PORT)
			.append(System.lineSeparator())
			.append(CommandListToFile.WAIT + ' ')
			.append(peerCount * CommandListToFile.WAIT_TIME)
			.append(System.lineSeparator())
			.append(CommandListToFile.LIST_MESSAGES)
			.append(System.lineSeparator())
			.append(CommandListToFile.WAIT + ' ')
			.append(CommandListToFile.WAIT_TIME * (peerCount*5))
			.append(System.lineSeparator())
			.append(CommandListToFile.EXIT);
		return commandList.toString();
	}

}
