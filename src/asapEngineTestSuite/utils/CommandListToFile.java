package asapEngineTestSuite.utils;

import asapEngineTestSuite.testScenarios.ScenarioTCPChain;
import asapEngineTestSuite.testScenarios.ScenarioTCPStar;
import asapEngineTestSuite.utils.fileUtils.FileUtils;

import java.io.*;
import static java.lang.Long.parseLong;
import static asapEngineTestSuite.testScenarios.TestComponents.generateReceiveScenarioCommands;

public class CommandListToFile {

	public static final String SEND_MESSAGE = "sendMessage";
	public static final String FORMAT_DESC_FILE = "sn/file";
	public static final String OPEN_TCP = "openTCP";
	public static final String CLOSE_TCP = "closeTCP";
	public static final String CONNECT_TCP = "connectTCP";
	public static final int HOST_PORT = 9999;
	public static final String EXIT = "exit";
	public static final String LIST_MESSAGES = "lsMessages";
	public static final String WAIT = "wait";

	public static final String DEFAULT_HOST_ADDRESS = "localhost"; //change this to the host address

	public static final String RUN_SCENARIO_SUCCESS = "Peer Wrapper: Command list for distributing IP address and receiving runScenario written to: ";
	public static final String DEFAULT_PARAMETERS_STRING = "Using default parameters.";

//	private String currentIPAddress;

	private final ScenarioParamAllocation scenarioParamAllocation;

	public static final String SCENARIO_FILE_NAME = "runScenario";
	public static final String TEXT_FILE_EXTENSION = ".txt";
	public static final String RECEIVE_COMMANDS_FILE_NAME = "receiveCommands";

	public static final int WAIT_TIME = 6000;

	/**
	 * Constructor that initializes the CommandlistToFile object with the specified path to the info sheet.
	 * @param path the path to the info sheet
	 */
	public CommandListToFile(String path) {
		this.scenarioParamAllocation = new ScenarioParamAllocation(path);
	}

	/**
	 * Constructor that initializes the CommandlistToFile object with the default parameters.
	 * This constructor is used when no path is provided.
	 */
	public CommandListToFile() {
		this.scenarioParamAllocation = new ScenarioParamAllocation();
	}

	/**
	 * Returns the peer count for the current scenario.
	 *
	 * @return the number of peers in the scenario
	 */
	public int getPeerCount() {
		return scenarioParamAllocation.getPeerCount();
	}

	/**
	 * Returns the object that stores the test parameters and their assigning methods.
	 */
	public ScenarioParamAllocation getScenarioParamAllocation() {
		return scenarioParamAllocation;
	}

	/**
	 * Parses the command line arguments to set the scenario parameters.
	 * @param args the command line arguments
	 */
	public void argsParser(String[] args) {
		long size;

		if (args.length < 7) {
			try {
				if (!args[0].equalsIgnoreCase("default")) {
					scenarioParamAllocation.setHostIPAddress(args[0]);
					scenarioParamAllocation.setScenarioIndex(Integer.parseInt(args[1]));
					scenarioParamAllocation.setPeerCount(Integer.parseInt(args[2]));
					scenarioParamAllocation.setFileSize(args[3], args[4]);
					size = scenarioParamAllocation.calculateFileSize(parseLong(args[3]));
					scenarioParamAllocation.setFileNameToBeSent(args[5]);
					FileUtils.createSpecifiedFile(size, scenarioParamAllocation.getFileNameToBeSent());
				}
				System.out.println(DEFAULT_PARAMETERS_STRING);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println(args.length + " argument(s) provided.");
				System.err.println("Using default values for the missing parameters.");
			} catch (IllegalArgumentException e) {
				System.err.println("Invalid file size or size unit. Using default values.");
				scenarioParamAllocation.setFileSize(ScenarioParamAllocation.DEFAULT_FILE_SIZE, ScenarioParamAllocation.DEFAULT_FILE_SIZE_UNIT);
			} catch (IOException e) {
				System.err
					.println("Error creating file: " + e.getMessage());
			}
		}
	}

	/**
	 * Writes the scenario specific peer command list to files.
	 *
	 * @param peerCount the number of peers
	 * @throws IOException if an I/O error occurs
	 */
	public void runScenarioPrinter(int peerCount, String hostAdress) throws IOException, IllegalArgumentException {
		if (peerCount < 1) {
			throw new IllegalArgumentException("peer count cannot be 0");
		}
		for (int i = 1; i <= peerCount; i++) {
			File file = new File("runScenario" + i + ".txt");

			if (this.getScenarioParamAllocation().getScenarioIndex() == 0) {

				ScenarioTCPStar scenarioTCPStar = new ScenarioTCPStar(this);

				FileUtils.writeToFile(new FileOutputStream(file.getName()),
					scenarioTCPStar.generateRunScenarioCommands(i, hostAdress));

			} else if (this.getScenarioParamAllocation().getScenarioIndex() == 1) {

				ScenarioTCPChain scenarioTCPChain = new ScenarioTCPChain(this);

				FileUtils.writeToFile(new FileOutputStream(file.getName()),
					scenarioTCPChain.generateRunScenarioCommands(i, hostAdress));
			}
			if (file.exists())
				System.out.println("File created: " + file.getName());
		}

	}


	public void receiveCommandsToFile(String hostIPAddress) {
		if (hostIPAddress == null || hostIPAddress.isEmpty()) {
			hostIPAddress = CommandListToFile.DEFAULT_HOST_ADDRESS;
		}
		String receiveCommands = generateReceiveScenarioCommands(hostIPAddress);
		File file = new File(CommandListToFile.RECEIVE_COMMANDS_FILE_NAME + CommandListToFile.TEXT_FILE_EXTENSION);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			FileUtils.writeToFile(fos, receiveCommands);
			System.out.println(RUN_SCENARIO_SUCCESS + file.getName());
		} catch (IOException e) {
			System.err.println("Error writing to file: " + e.getMessage());
		}
	}

	/**
	 * Writes the host command list to a file.
	 * @param testHost the TestHost object to generate the command list
	 * @throws IOException if an I/O error occurs while creating the file
	 */
	public void hostCommandListToFile(TestHost testHost) throws IOException {
		File file = new File("hostCommandList.txt");
		FileOutputStream fos = new FileOutputStream(file);
		String commandList = testHost.hostCommandList(this);
		FileUtils.writeToFile(fos, commandList);
		System.out.println("Host command list written to file: " + file.getName());
	}

}
