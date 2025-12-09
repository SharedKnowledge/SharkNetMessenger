package net.sharksystem.app.messenger.commands.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.*;

public class SNMessagesSerializer {
    public static void serializeFile(String fileName, OutputStream os) throws IOException, SharkException {
        //file name (CharacterSequence), file length (long), file content (byte[])

        File file2Send = new File(fileName);
        if(!file2Send.exists()) throw new IOException("file does not exist: " + file2Send);

        if(file2Send.length() > Integer.MAX_VALUE) {
            throw new SharkException("file to long - length limited to Integer.MAX_VALUE in this implementation");
        }

        String shortFileName = file2Send.getName();
        ASAPSerialization.writeCharSequenceParameter(shortFileName, os);
        int length = (int) file2Send.length();
        ASAPSerialization.writeIntegerParameter(length, os);
        FileInputStream fis = new FileInputStream(file2Send);
        for(int i = 0; i < file2Send.length(); i++) os.write(fis.read());
        fis.close();
    }

    public static SNFileMessage deserializeFile(byte[] serializedFile) throws IOException, SharkException {
        return SNMessagesSerializer.deserializeFile(new ByteArrayInputStream(serializedFile));
    }

    public static SNFileMessage deserializeFile(InputStream is) throws IOException, SharkException {
        return new SNFileMessage(
                ASAPSerialization.readCharSequenceParameter(is),
                ASAPSerialization.readIntegerParameter(is),
                is);
    }

    public static class SNFileMessage {
        private final String fileName;
        private final int size;
        private final InputStream is;
        private byte[] fileContent;

        public String getFileName() { return this.fileName;}

        public int getSize() { return this.size;}

        public byte[] getFileContent() throws IOException {
            if(this.fileContent == null) {
                this.fileContent = new byte[this.size];
                this.is.read(this.fileContent);

            }
            return this.fileContent;
        }

        private SNFileMessage(String fileName, int size, InputStream is) {
            this.fileName = fileName;
            this.size = size;
            this.is = is;
        }
    }
}
