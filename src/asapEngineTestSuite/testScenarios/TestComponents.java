package asapEngineTestSuite.testScenarios;

import asapEngineTestSuite.utils.ScenarioIndex;
import asapEngineTestSuite.utils.CommandListToFile;
import asapEngineTestSuite.utils.ScenarioParamAllocation;

public abstract class TestComponents extends ScenarioParamAllocation{

	protected String printSuccess = "";
	private final ScenarioIndex si = null;

	public String getPrintSuccess() {
		return printSuccess;
	}

	private final CommandListToFile commandListToFile;

	public TestComponents(CommandListToFile clf) {
		commandListToFile = clf;
	}

	public CommandListToFile getCommandListToFile() {
		return commandListToFile;
	}

	public abstract int getScenarioIndex();

	/**
	 * Generates a command list for receiving messages in a TCP scenario.
	 *
	 * @param hostIPAddress the IP address of the host to connect to
	 * @return the command list as a string
	 */
	public static String generateReceiveScenarioCommands(String hostIPAddress) {
		return sendIPAddressCommandList() +
			CommandListToFile.WAIT + ' ' + 1000 +
			System.lineSeparator() +
			CommandListToFile.CONNECT_TCP + " " +
			hostIPAddress + ' ' +
			CommandListToFile.HOST_PORT +
			System.lineSeparator() +
			CommandListToFile.WAIT + ' ' + 500 +
			System.lineSeparator() +
			CommandListToFile.LIST_MESSAGES +
			System.lineSeparator() +
			CommandListToFile.WAIT + ' ' + 5000 +
			System.lineSeparator() +
			CommandListToFile.EXIT;
	}

	/**
	 * Generates a command list to send a file to the test host containing an IP address.
	 *
	 * @return the command list as a string
	 */
	static String sendIPAddressCommandList() {
		String sb = CommandListToFile.SEND_MESSAGE +
			' ' +
			"ipAddress_" +
			"FILLER_PEERNAME" +
			CommandListToFile.TEXT_FILE_EXTENSION +
			' ' +
			CommandListToFile.FORMAT_DESC_FILE +
			System.lineSeparator();
		return sb;
	}

	/**
	 * Generates a command list for the wrappers to execute that sends the logs to the test host.
	 *
	 * @param peerIndex the index of the peer
	 * @param hostAddress the IP address of the host to connect to
	 * @return the command list as a string
	 * @throws IllegalArgumentException if the peer index is invalid
	 */
	public String generateRunScenarioCommands(int peerIndex, String hostAddress) {
		int peerCount = getPeerCount();
		if (peerIndex < 1 || peerIndex > peerCount) {
			throw new IllegalArgumentException("Invalid peer index: " + peerIndex);
		}
		if (hostAddress == null || hostAddress.isEmpty()) {
			hostAddress = CommandListToFile.DEFAULT_HOST_ADDRESS;
		}
		StringBuilder stringBuilder = new StringBuilder();
		StringBuilder scenarioScript = stringBuilder
			.append(CommandListToFile.WAIT + ' ')
			.append(100 * peerCount)
			.append(System.lineSeparator())
			.append(CommandListToFile.SEND_MESSAGE).append(" snm_P")
			.append(peerIndex)
			.append(".txt ")
			.append(CommandListToFile.FORMAT_DESC_FILE)
			.append(System.lineSeparator())
			.append(CommandListToFile.SEND_MESSAGE)
			.append(" asapLogsP")
			.append(peerIndex)
			.append(".txt ")
			.append(CommandListToFile.FORMAT_DESC_FILE)
			.append(System.lineSeparator())
			.append(CommandListToFile.CONNECT_TCP + ' ')
			.append(hostAddress)
			.append(' ')
			.append(CommandListToFile.HOST_PORT)
			.append(System.lineSeparator())
			.append(CommandListToFile.WAIT + ' ' + 1000)
			.append(System.lineSeparator())
			.append(CommandListToFile.EXIT);
		return scenarioScript.toString();
	}

	public abstract void sendMessage(StringBuilder sb, String s);
	public abstract void waitAndOpenPort(int pc, StringBuilder sb);
	public abstract void waitAndConnect(int pc, StringBuilder sb);
	public void waitAndListMessages(StringBuilder stringBuilder){}

	protected String generateTestScenarioCommands(int peerIndex) throws IllegalArgumentException
	{
		int peerCount = commandListToFile.getScenarioParamAllocation().getPeerCount();
		if (peerIndex < 1 || peerIndex > peerCount) {
			throw new IllegalArgumentException("Invalid peer index: " + peerIndex);
		}
		// a waiting period in milliseconds
		String peerSpecificFileNameToBeSent = peerIndex + "_" + this.commandListToFile.getScenarioParamAllocation().getFileNameToBeSent();
		//this block consists of the common commands for all peers.
		StringBuilder scenarioScript = new StringBuilder();
		if (peerIndex == peerCount) {
			sendMessage(scenarioScript, peerSpecificFileNameToBeSent);
		}
		if (peerIndex < peerCount - 1) {
			waitAndOpenPort(peerIndex, scenarioScript);
		}
		if (peerIndex > 1) {
			waitAndConnect(peerIndex, scenarioScript);
		}
		waitAndListMessages(scenarioScript);
		return scenarioScript.toString();
	}
}