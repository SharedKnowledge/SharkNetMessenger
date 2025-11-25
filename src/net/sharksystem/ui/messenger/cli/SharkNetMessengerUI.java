package net.sharksystem.ui.messenger.cli;

import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestion;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;

import java.io.*;
import java.util.*;

/**
 * This class contains the UI logic and is the interface between user
 * and application.
 */
public class SharkNetMessengerUI {

    private static final String UNDERLINE = "---------\n";
    private static final String COMMAND_SEPARATOR = ";";
    private static boolean isInteractive = false;

    private final Map<String, UICommand> commands = new HashMap<>();
    private List<String> commandParameterStrings = new ArrayList<>();
    private final PrintStream outStream;
    private final PrintStream errStream;
    private final BufferedReader bufferedReader;
    private final List<String> parsedCommands = new ArrayList<>();

    /**
     * Use for string input in unittests or no input
     */
    public SharkNetMessengerUI(
            String batchCommands, InputStream is, PrintStream out, PrintStream err) {
        this.parsedCommands.addAll(Arrays.asList(batchCommands.trim().split(System.lineSeparator())));
        this.outStream = out;
        this.errStream = err;
        this.bufferedReader = new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Use for text file as batch input
     *
     * @throws FileNotFoundException
     */
    public SharkNetMessengerUI(File file, InputStream is, PrintStream out, PrintStream err) throws FileNotFoundException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(file)))
                .lines().reduce("", (a, b) -> a + System.lineSeparator() + b), is, out, err
        );
    }

    /**
     * Takes user input and executes the given command with its arguments.
     * The input needs to follow the following scheme:
     * <command> <argument1> <argument2> ... <argumentN>
     * <p>
     * If invalid arguments are provided the command will not execute.
     * For the needed arguments visit the specific commands description.
     * @param input the user input String.
     * @throws Exception if a command encountered a critical error.
     */
    public void handleUserInput(String input) throws Exception {
        List<String> cmd = optimizeUserInputString(input);
        String commandIdentifier = cmd.remove(0);

        if (!commands.containsKey(commandIdentifier)) {
            this.commandNotFound(commandIdentifier);
            return;
        }

        UICommand command = this.commands.get(commandIdentifier);

        // Interactive mode uses the old questionnaire to obtain its arguments.
        if (isInteractive) {
            command.startCommandExecution();
        } else {
            boolean executed = command.execute(cmd);
            if(!executed) {
                this.errStream.println("Arguments invalid for command: " + commandIdentifier);
            } else {
                if (command.rememberCommand()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(command.getIdentifier());
                    // append parameter
                    for (String parameter : cmd) {
                        sb.append(" ");
                        sb.append(parameter);
                    }
                    this.addCommandToHistory(sb.toString());
                }
            }
        }
    }

    // TODO: At the moment this is not used and there is no way to set flags.
    /**
     * Sets the specified flags for the application.
     * It searches the given String arguments for a hyphen
     * and uses the following characters as flags.
     * <p>
     * Available flags:
     * <p>
     * <pre>-i   interactive mode, uses the questionaire.</pre>
     * @param args
     */
    public static void setFlags(String[] args) {
        final String hyphen = "-";

        for (String argument : args) {
            argument.strip();
            if (argument.startsWith(hyphen)) {
                for (int i = 1; i < argument.length(); i++) {
                    char flag = argument.charAt(i);

                    switch (flag) {
                        case 'i':
                            isInteractive = true;
                    }
                }
            }
        }
    }

    public List<String> getParsedCommands() {
        return this.parsedCommands;
    }

    public void addCommandToHistory(String commandIdentifier) {
        this.commandParameterStrings.add(commandIdentifier);
    }

    public List<String> getCommandHistory() {
        return this.commandParameterStrings;
    }

    public Map<String, UICommand> getCommands() {
        return this.commands;
    }

    private Map<String, List<UICommand>> commandsByGroup = null;

    public List<UICommand> getCommands(String group) {
        return this.commandsByGroup.get(group);
    }

    public Set<String> getCommandGroups() {
        return this.getCommandsByGroup().keySet();
    }

    private Map<String, List<UICommand>> getCommandsByGroup() {
        if(this.commandsByGroup == null) {
            this.commandsByGroup = new HashMap<>();
            // get all unsorted command
            Collection<UICommand> commands = this.getCommands().values();
            for(UICommand command : commands) {
                String groupName = command.getClass().getPackageName();
                // extract last package name
                int lastDot = groupName.lastIndexOf('.');
                groupName = groupName.substring(lastDot+1);

                List<UICommand> commandList = this.commandsByGroup.get(groupName);

                if(commandList == null) {
                    // new group
                    commandList = new ArrayList<>();
                    this.commandsByGroup.put(groupName, commandList);
                }
                commandList.add(command);
            }
        }
        return this.commandsByGroup;
    }

    public void logQuestionAnswer(String userInput) {
        this.addCommandToHistory(userInput);
    }

    public void addCommand(UICommand command) {
        this.commands.put(command.getIdentifier(), command);

        // tell command its print stream
        command.setPrintStream(this.outStream);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                         actual user interface code                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean letUserFillOutQuestionnaire(UICommandQuestionnaire questionnaire) {
        for (UICommandQuestion question : questionnaire.getQuestions()) {
            String userInput = "";
            try {
                do {
                    this.outStream.print(question.getQuestionText());
                    try {
                        userInput = bufferedReader.readLine();
                        if (userInput.equals(UICommandQuestionnaire.EXIT_SEQUENCE)) {
                            this.addCommandToHistory(UICommandQuestionnaire.EXIT_SEQUENCE);
                            return false;
                        }
                    } catch (IOException e) {
                        this.printError(e.getLocalizedMessage());
                    }
                } while (!question.submitAnswer(userInput));
            } catch (Exception e) {
                this.printError(e.getLocalizedMessage());
            }
            this.printRecall(userInput);
            this.addCommandToHistory(userInput);
        }
        return true;
    }

    private void printRecall(String output) {
        this.outStream.println("> " + output);
    }

    public void printError(String error) {
        this.errStream.println("exception: " + error);
    }

    public void commandWasTerminated(String identifier) {
        this.outStream.println("The following command was terminated: " + identifier);
    }

    /**
     * Prints a message for the user to let him know, the command he gave is
     * not on the list of commands.
     * @param commandIdentifier
     */
    public void commandNotFound(String commandIdentifier) {
        String sb = "Unknown command: " +
                commandIdentifier +
                System.lineSeparator() +
                "Type 'help' to see the list of commands.";
        this.outStream.println(sb);
    }

    public PrintStream getOutStream() {
        return this.outStream;
    }

    public PrintStream getErrStream() {
        return this.errStream;
    }

    // would be nice to organize command list (group test commands)
    public void printUsage() {
        StringBuilder sb = new StringBuilder();

        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("COMMANDS:");
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());

        Set<String> commandGroups = this.getCommandGroups();
        for(String commandGroup : commandGroups) {
            sb.append(commandGroup);
            sb.append(System.lineSeparator());
            sb.append(UNDERLINE);

            // calc length for a pretty layout
            int longestCmd = 0;
            for (UICommand cmd : this.getCommands().values()) {
                int curLength = cmd.getIdentifier().length();
                if (curLength > longestCmd) {
                    longestCmd = curLength;
                }
            }

            // produce output
            for (UICommand cmd : this.getCommands(commandGroup)) {
                sb.append(cmd.getIdentifier());
                sb.append(" ".repeat(Math.max(0, longestCmd - cmd.getIdentifier().length())));
                sb.append("\t");
                sb.append(cmd.getDescription());
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }

        this.outStream.println(sb);
    }

    /**
     * Starts the command loop which begins taking user input until stopped by
     * other means.
     */
    public void runCommandLoop() {
        boolean running = true;
        String commandsBuffer = null;
        if(!this.parsedCommands.isEmpty()) {
            boolean roundOne = true;
            for(String command : this.parsedCommands) {
                if (roundOne) {
                    roundOne = false;
                    commandsBuffer = command;
                } else {
                    commandsBuffer += ";";
                    commandsBuffer += command;
                }
            }
        }
        while (running) {
            try {
                String userInputString;
                if(commandsBuffer == null) {
                    this.outStream.println();
                    this.outStream.print("Enter a (semicolon separated) command (list) and press ENTER > ");

                    userInputString = this.bufferedReader.readLine();
                } else {
                    userInputString = commandsBuffer;
                }

                if(userInputString == null) {
                    running = false;
                    this.errStream.println("input null - going to wait a sec and exit.");
                    // give app a moment so finish threads.
                    Thread.sleep(1000);
                    System.exit(1);
                }

                int indexSeparator = userInputString.indexOf(COMMAND_SEPARATOR);
                if(indexSeparator != -1) {
                    commandsBuffer = userInputString.substring(indexSeparator+1).trim();
                    if(commandsBuffer.length() == 0) commandsBuffer = null;
                    userInputString = userInputString.substring(0, indexSeparator);
                } else {
                    commandsBuffer = null;
                }


                if (userInputString != null) {
                    this.handleUserInput(userInputString);
                }

            } catch (Exception e) {
                this.errStream.println("exception caught: " + e.getLocalizedMessage());
                e.printStackTrace(errStream);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                    Helpers                                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Converts a string representing a command input by the user into a list of all command arguments.
     * All arguments are freed up from any spaces. All elements of the list are in lower case.
     * Example: (upper cases, too many spaces)
     * > gIt   hElp
     * > {"git", "help"}
     *
     * @param input the string inputted by the user
     * @return a list of all command arguments
     */
    private List<String> optimizeUserInputString(String input) {
        List<String> cmd = new ArrayList<>();
        final String space = " ";

        String[] unfinishedCmd = input.split(space);
        for (String attribute : unfinishedCmd) {
            //attribute = attribute.toLowerCase();
            attribute = attribute.trim();

            if (!attribute.equals(space)) {
                cmd.add(attribute);
            }
        }
        return cmd;
    }

    public void clearCommandHistory() {
        this.commandParameterStrings = new ArrayList<>();
    }
}
