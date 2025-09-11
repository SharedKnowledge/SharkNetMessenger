package asapEngineTestSuite.testScenarios;


import asapEngineTestSuite.utils.ScenarioIndex;
import asapEngineTestSuite.utils.CommandListToFile;
import asapEngineTestSuite.utils.fileUtils.FileUtils;

import java.io.FileOutputStream;


public class ScenarioTCPChain extends TestComponents {
	/**
	 * The scenario index for the TCP Chain scenario.
	 */
	public static final ScenarioIndex SCENARIO_INDEX = ScenarioIndex.TCP_CHAIN;

	/**
	 * The starting TCP port number for the scenario.
	 */
	private static final int STARTING_TCP_PORT = 4443;

	/**
	 * The wait time in milliseconds between commands.
	 */
	private static final int WAIT_TIME_IN_MILLIS = 8000;

	public String getPrintSuccess() {
		return "Scenario: TCP Chain commands generated.";
	}

	public ScenarioTCPChain(CommandListToFile commandListToFile) {
		super(commandListToFile);
	}

	@Override
	public int getScenarioIndex() {
		return SCENARIO_INDEX.ordinal();
	}

	public void sendMessage(StringBuilder scenarioScript, String peerSpecificFileNameToBeSent) {
		scenarioScript
			.append(CommandListToFile.SEND_MESSAGE)
			.append(" ")
			.append(peerSpecificFileNameToBeSent)
			.append(' ')
			.append(CommandListToFile.FORMAT_DESC_FILE)
			.append(System.lineSeparator());
	}

	public void waitAndOpenPort(int peerIndex, StringBuilder scenarioScript) {
		scenarioScript
			.append(CommandListToFile.WAIT + ' ')
			.append(WAIT_TIME_IN_MILLIS * 100)
			.append(System.lineSeparator())
			.append(CommandListToFile.OPEN_TCP + ' ')
			.append(STARTING_TCP_PORT + peerIndex)
			.append(System.lineSeparator());
	}

	public void waitAndConnect(int peerIndex, StringBuilder scenarioScript) {
		scenarioScript
			.append(CommandListToFile.WAIT + ' ')
			.append(WAIT_TIME_IN_MILLIS * 100)
			.append(System.lineSeparator())
			.append(CommandListToFile.CONNECT_TCP + ' ')
			.append("FILLER_PREVIOUS_PEER")
			.append(' ')
			.append(STARTING_TCP_PORT + (peerIndex - 1))
			.append(System.lineSeparator());
	}

	public void waitAndListMessages(StringBuilder scenarioScript) {
		scenarioScript.append(CommandListToFile.WAIT + ' ')
				.append(WAIT_TIME_IN_MILLIS * 100)
				.append(System.lineSeparator())
				.append(CommandListToFile.LIST_MESSAGES)
				.append(System.lineSeparator())
				.append(CommandListToFile.EXIT);
	}

	public void testScenarioCommandsToFile(int peerCount) {
		for (int i = 1; i <= peerCount; i++) {
			String commands = generateTestScenarioCommands(i);
			try {
				String fileName = "scenarioScriptP" + i + ".txt";
				FileUtils.writeToFile(new FileOutputStream(fileName), commands);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
