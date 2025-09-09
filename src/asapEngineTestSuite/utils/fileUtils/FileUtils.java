package asapEngineTestSuite.utils.fileUtils;

import java.io.*;

public class FileUtils {

	/**
	 * Writes the given string to a file.
	 * @param fos the file output stream
	 * @param goal the string to write
	 * @throws IOException if an I/O error occurs
	 */
	public static void writeToFile(FileOutputStream fos, String goal) throws IOException {
		fos.write(goal.getBytes());
		fos.close();
	}

	public static BufferedReader getBufferedReader(String filepath) throws FileNotFoundException {
		return new BufferedReader(new FileReader(filepath));
	}

	public static void createSpecifiedFile(long filesize, String filename) throws IOException {
		try(RandomAccessFile file = new RandomAccessFile(new File(filename), "rw")) {
			file.setLength(filesize);
			System.out.println("File created: " + filename + " with size: " + filesize + " bytes");
		} catch (IOException e) {
			System.err.println("Error creating file: " + e.getMessage());
		}
	}
}