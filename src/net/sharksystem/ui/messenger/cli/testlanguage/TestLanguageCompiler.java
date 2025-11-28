package net.sharksystem.ui.messenger.cli.testlanguage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestLanguageCompiler {
    public static final char CONNECT_COMMAND_SIGN = 'C';
    public static final char LANGUAGE_SEPARATOR = ';';

    public static final char CLI_SPACE = ' ';
    public static final char CLI_SEPARATOR = ';';

    public static final String DEFAULT_PORT = "6907";

    public static final String CLI_OPEN_TCP = "openTCP";
    public static final String CLI_CONNECT_TCP = "connectTCP";
    private static final String CLI_BLOCK = "block";
    private static final String CLI_RELEASE = "release";
    private static final String CLI_EXIT = "exit";

    /**
     * sentence -> command SEPARATOR command
     * command -> connectCommand | ...
     * connectCommand -> CONNECT_COMMAND_SIGN  peer peer
     * peer -> 'a', 'b'
     */

    public Map<String, String> compile(String sentence) throws TestLanguageCompilerException {
        Map<String, StringBuilder> peerScripts = new HashMap<>();

        List<String> commandList = this.getCommandList(sentence);

        for(String commandString : commandList) {
            char commandSign = commandString.charAt(0);
            String parameterString = commandString.substring(1);
            switch (commandSign) {
                case CONNECT_COMMAND_SIGN -> {
                    try {
                        this.parseConnectCommand(peerScripts, parameterString);
                    } catch (TestLanguageCompilerException e) {
                        System.err.println("while parsing: " + commandSign + parameterString);;
                        System.err.println(e.getLocalizedMessage());
                        throw e;
                    }
                }
            }
        }

        // done, finalise scripts
        Map<String, String> scripts = new HashMap<>();
        for(String peerName : peerScripts.keySet()) {
            StringBuilder sb = peerScripts.get(peerName);
            sb.append(CLI_SEPARATOR);
            sb.append(CLI_EXIT);
            sb.append(CLI_SEPARATOR);
            scripts.put(peerName, sb.toString());
        }
        return scripts;
    }

    private void parseConnectCommand(Map<String, StringBuilder> peerScripts, String parameterString) throws TestLanguageCompilerException {
        // parse Parameter - there must be exactly two peer names
        String[] peerNames = this.getTwoPeerNames(parameterString);
        StringBuilder scriptBuilderA = this.getScriptBuilder(peerScripts, peerNames[0]);
        StringBuilder scriptBuilderB = this.getScriptBuilder(peerScripts, peerNames[1]);

        // Cab

        // A:
        // open prot
        scriptBuilderA.append(CLI_OPEN_TCP);
        scriptBuilderA.append(CLI_SPACE);
        scriptBuilderA.append(DEFAULT_PORT);
        scriptBuilderA.append(CLI_SEPARATOR);

        // wait until b connected
        scriptBuilderA.append(CLI_BLOCK);
        scriptBuilderA.append(CLI_SPACE);
        String synchronisationPointName = this.getNewSynchronisationPoint();
        scriptBuilderA.append(synchronisationPointName);
        scriptBuilderA.append(CLI_SEPARATOR);

        // B:
        scriptBuilderB.append(CLI_CONNECT_TCP);
        scriptBuilderB.append(CLI_SPACE);
        scriptBuilderB.append("localhost"); // TODO
        scriptBuilderB.append(CLI_SPACE);
        scriptBuilderB.append(DEFAULT_PORT);
        scriptBuilderB.append(CLI_SEPARATOR);

        scriptBuilderB.append(CLI_RELEASE);
        scriptBuilderB.append(CLI_SPACE);
        scriptBuilderB.append(synchronisationPointName);
        scriptBuilderB.append(CLI_SEPARATOR);
    }

    private int syncPointIndex = 0;
    private String getNewSynchronisationPoint() {
        return "S" + this.syncPointIndex++;
    }

    private StringBuilder getScriptBuilder(Map<String, StringBuilder> peerScripts, String peerName) {
        StringBuilder sb = peerScripts.get(peerName);
        if(sb == null) {
            sb= new StringBuilder();
            peerScripts.put(peerName, sb);
        }
        return sb;
    }

    private String[] getTwoPeerNames(String parameterString) throws TestLanguageCompilerException {
        if(parameterString.length() != 2) {
            throw new TestLanguageCompilerException("two peer signs expected: " + parameterString);
        }
        return new String[]{parameterString.substring(0,1), parameterString.substring(1)};
    }

    private List<String> getCommandList(String sentence) {
        // split sentence into commands
        int newSeparatorIndex = sentence.indexOf(LANGUAGE_SEPARATOR);
        List<String> commandStrings = new ArrayList<>();

        if(newSeparatorIndex == -1) {
            // last command
            commandStrings.add(sentence);
        }
        else do {
            // skip SEPARATOR(s)
            while(newSeparatorIndex == 0) {
                if(sentence.length() == 1) {
                    newSeparatorIndex = -1;
                    break;
                }
                sentence = sentence.substring(1);
                newSeparatorIndex = sentence.indexOf(LANGUAGE_SEPARATOR);
            }
            String commandString = null;
            if(newSeparatorIndex > -1) commandString = sentence.substring(0,newSeparatorIndex);
            else commandString = sentence;

            commandStrings.add(commandString);

            sentence = sentence.substring(commandString.length());
            newSeparatorIndex = sentence.indexOf(LANGUAGE_SEPARATOR);
        } while(newSeparatorIndex > -1);

        return commandStrings;
    }
}
