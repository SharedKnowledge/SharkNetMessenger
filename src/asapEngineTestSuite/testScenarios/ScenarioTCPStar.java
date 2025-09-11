package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.ScenarioIndex;
import asapEngineTestSuite.utils.CommandListToFile;
import asapEngineTestSuite.utils.fileUtils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScenarioTCPStar extends TestComponents {
	public static final ScenarioIndex SCENARIO_INDEX = ScenarioIndex.TCP_STAR;

	private final String printSuccess = "Scenario: TCP Star commands generated." ;

	/**
	 * The TCP port used by peer1 to listen for incoming connections.
	 */
	private static final int TCP_PORT_PEER1 = 4444;

	/**
	 * Constructor for the ScenarioTCPStar class.
	 * Assigns the CommandListToFile instance to the commandListToFile variable.
	 * @param commandListToFile the CommandListToFile instance containing the scenario parameters
	 */
	public ScenarioTCPStar(CommandListToFile commandListToFile) {
		super(commandListToFile);
	}

	@Override
	public int getScenarioIndex() {
		return SCENARIO_INDEX.ordinal();
	}

	/**
	 * Generates a commandlist for the peers to execute the test scenario: TCP, direct communication with peer1.
	 *
	 * @param peerIndex the index of the peer
	 * @return the commandlist as a string
	 */
	@Override
	public String generateTestScenarioCommands(int peerIndex) throws IllegalArgumentException {
		if (peerIndex < 1 || peerIndex > getPeerCount()) {
			throw new IllegalArgumentException("Invalid peer index: " + peerIndex);
		}
		String filename = peerIndex + "_" + getCommandListToFile().getScenarioParamAllocation().getFileNameToBeSent();

		StringBuilder stringBuilder = new StringBuilder();
		sendMessage(stringBuilder, filename);
		if (peerIndex == 1) {
			waitAndOpenPort(1, stringBuilder);
		}
		if (peerIndex > 1) {
			waitAndConnect(peerIndex, stringBuilder);
			//wait for the host to open the TCP port
		}
		stringBuilder
			.append(System.lineSeparator())
			.append(CommandListToFile.EXIT);
		return stringBuilder.toString();
	}

	public void waitAndConnect(int peerindex, StringBuilder stringBuilder) {
		stringBuilder
			.append(CommandListToFile.CONNECT_TCP).append(' ')
			.append("ADDRESS_PEER1").append(' ')
			.append(TCP_PORT_PEER1)
			.append(System.lineSeparator())
			.append(CommandListToFile.WAIT).append(' ')
			.append(10000);
	}

	public void waitAndOpenPort(int peerindex, StringBuilder stringBuilder) {
		stringBuilder.append(CommandListToFile.OPEN_TCP).append(' ')
			.append(TCP_PORT_PEER1)
			.append(System.lineSeparator())
			.append(CommandListToFile.WAIT).append(' ')
			.append(150000);
	}

	public void sendMessage(StringBuilder stringBuilder, String filename) {
		stringBuilder.append(CommandListToFile.SEND_MESSAGE)
			.append(" ")
			.append(filename)
			.append(" ")
			.append(CommandListToFile.FORMAT_DESC_FILE)
			.append(System.lineSeparator());
	}

	public void testScenarioCommandsToFile(int peerCount) {
		for (int i = 1; i <= peerCount; i++) {
			String commands = generateTestScenarioCommands(i);
			try {
				String fileName = "scenarioScriptP" + i + ".txt";
				File file = new File(fileName);
				if (!file.exists() && file.createNewFile()) {
					FileUtils.writeToFile(new FileOutputStream(fileName), commands);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
