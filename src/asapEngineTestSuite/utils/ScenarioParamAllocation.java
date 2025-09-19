package asapEngineTestSuite.utils;

import asapEngineTestSuite.utils.fileUtils.FileUtils;

import java.io.*;

import static java.lang.Long.parseLong;

/**
 * This class is responsible for parsing the info sheet and generating the command list for the peers.
 * It reads the command list from a file and assigns the parameters to the corresponding variables.
 * It also generates a command list for the peers to execute the test scenario.
 * Alternatively, the test variables can be passed as command line arguments.
 * @author Cemre
 */
public class ScenarioParamAllocation {
	public static final int DEFAULT_PEER_COUNT = 5;
	public static final String DEFAULT_FILE_NAME = "200Kb.txt";
	public static final String DEFAULT_FILE_SIZE = "" + (1024 * 2); // 200 kB in bytes
	public static final String DEFAULT_FILE_SIZE_UNIT = "kB";
	public static final ScenarioIndex DEFAULT_SCENARIO_INDEX = ScenarioIndex.TCP_STAR;

	ScenarioIndex[] scenarios = ScenarioIndex.values();

	//public static final String SCENARIO_HOST_NAME = "testHost"; //oder so

	private int peerCount = DEFAULT_PEER_COUNT;
	private String fileNameToBeSent = DEFAULT_FILE_NAME;
	private String fileSizeUnit = DEFAULT_FILE_SIZE_UNIT;
	private ScenarioIndex scenarioIndex = DEFAULT_SCENARIO_INDEX;
	private String hostIPAddress = CommandListToFile.DEFAULT_HOST_ADDRESS;
	private long fileSize = 1024 * 2; // 200 kB in bytes

	/**
	 * Default constructor that initializes the utils.ScenarioParamAllocation object with default values.
	 * This constructor should be used for command line argument parsing.
	 */
	public ScenarioParamAllocation() {
	}

	/**
	 * Constructor that initializes the InfoSheetParser with the given file path.
	 *
	 * @param pathInfoSheet the path to the info sheet file
	 */
	public ScenarioParamAllocation(String pathInfoSheet) {
		try (BufferedReader bufferedReader = FileUtils.getBufferedReader(pathInfoSheet)) {
			assignCommandListComponents(bufferedReader);
		} catch (IOException e) {
			System.err.println("Cannot read the info sheet: " + e.getMessage());
		}
	}

	/**
	 * Sets the file name to be sent.
	 *
	 * @param fileNameToBeSent the name of the file to be sent
	 */
	public void setFileNameToBeSent(String fileNameToBeSent) {
		if (fileNameToBeSent == null || fileNameToBeSent.isEmpty()) {
			System.err.println("File name unspecified. Using default file name: " + DEFAULT_FILE_NAME);
			return;
		}
		this.fileNameToBeSent = fileNameToBeSent;
	}

	public void setHostIPAddress(String address) {
		if (address == null || address.isEmpty()) {
			System.err.println("Host IP address unspecified. Using default host IP address: " + CommandListToFile.DEFAULT_HOST_ADDRESS);
			return;
		}
		this.hostIPAddress = address;
	}

	/**
	 * Sets the file size.
	 *
	 * @param fileSize the size of the file
	 */
	public void setFileSize(String fileSize, String fileSizeUnit) {
		this.fileSize = parseLong(fileSize);
		if (this.fileSize == 0) {
			System.err.println("File size unspecified. Using default file size: " + DEFAULT_FILE_SIZE);
			return;
		}
		if (fileSizeUnit == null || fileSizeUnit.isEmpty()) {
			System.err.println("File size unit unspecified. Using default file size unit: " + DEFAULT_FILE_SIZE_UNIT);
			return;
		}
		if (this.fileSize <= 0) {
			System.err.println("File size invalid. Using default file size: " + DEFAULT_FILE_SIZE);
			return;
		}
		setFileSizeUnit(fileSizeUnit);
		this.fileSize = calculateFileSize(parseLong(fileSize));

	}

	/**
	 * Sets the peer count.
	 *
	 * @param peerCount the number of peers
	 */
	public void setPeerCount(int peerCount) {
		if (peerCount <= 0) {
			System.err.println("Peer count invalid. Using default peer count: " + DEFAULT_PEER_COUNT);
			return;
		}
		this.peerCount = peerCount;
	}

	/**
	 * Sets the scenario index.
	 *
	 * @param scenarioIndex the index of the scenario
	 */
	public void setScenarioIndex(int scenarioIndex) {
		this.scenarioIndex = scenarios[scenarioIndex];

	}

	/**
	 * Reads the command list from the given file and assigns the parameters to the corresponding variables.
	 *
	 * @throws IOException if the file cannot be read
	 */
	public void assignCommandListComponents(BufferedReader bufferedReader) throws IOException {
		try {
			String nextLine = bufferedReader.readLine();
			while (nextLine != null) {
				String keyValuePair = nextLine;
				String key = keyValuePair.substring(0, keyValuePair.indexOf(":")).trim();
				String value = keyValuePair.substring(keyValuePair.indexOf(":") + 1).trim();

				switch (key.toLowerCase()) {
					case "peer count":
						setPeerCount(Integer.parseInt(value));
						break;
					case "scenario index":
						setScenarioIndex(Integer.parseInt(value));
						break;
					case "file name":
						setFileNameToBeSent(value);
						break;
					case "file size":
						String unit = value.substring(value.length() - 2);
						String size = value.substring(0, value.indexOf(unit)).trim();
						setFileSize(size, unit);
						break;
					case   "host ip address", "host address":
						if (value.isEmpty()) {
							System.err.println("Host IP address unspecified. Using default host IP address: " + CommandListToFile.DEFAULT_HOST_ADDRESS);
							return;
						}
						setHostIPAddress(value);
						break;
					default:
						System.err.println("Unknown key: " + key);
						break;
				}
				nextLine = bufferedReader.readLine();
			}
		} catch (FileNotFoundException e) {
			System.err.println("Infosheet: file not found. Test will run with default parameters.");
		}
	}

	/**
	 * Sets the file size unit.
	 *
	 * @param value the unit of the file size
	 */
	private void setFileSizeUnit(String value) {
		if (value == null || value.isEmpty()) {
			System.err.println("File size unit unspecified. Using default file size unit: " + DEFAULT_FILE_SIZE_UNIT);
			return;
		}
		this.fileSizeUnit = value;
	}

	/**
	 * Returns the scenario index.
	 *
	 * @return file name to be sent
	 */
	public String getFileNameToBeSent() {
		return fileNameToBeSent;
	}

	/**
	 * Returns the file size.
	 *
	 * @return file size
	 */
	public int getPeerCount() {
		return peerCount;
	}

	/**
	 * Return the IP address of the test host
	 * @return host IP address
	 */
	public String getHostIPAddress() {
		return hostIPAddress;
	}

	/**
	 * Returns the scenario index.
	 *
	 * @return scenario index
	 */
	public int getScenarioIndex() {
		return scenarioIndex.ordinal();
	}

	/**
	 * Calculates the file size in bytes based on the given file size and unit.
	 * @param fileSize the file size to be converted
	 * @return the file size in bytes
	 */
	public long calculateFileSize(long fileSize) {
		this.fileSize = fileSize;
		if (fileSize <= 0)
			return parseLong(DEFAULT_FILE_SIZE);
		switch (fileSizeUnit.toUpperCase()) {
			case "KB":
				fileSize *= 1024;
				break;
			case "MB":
				fileSize *= 1024 * 1024;
				break;
			case "GB":
				fileSize *= 1024 * 1024 * 1024;
				break;
			default:
				System.err.println("Invalid file size unit. Using " + DEFAULT_FILE_SIZE_UNIT);
		}
		return fileSize;
	}
}