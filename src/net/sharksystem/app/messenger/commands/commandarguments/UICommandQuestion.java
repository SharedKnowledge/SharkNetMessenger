package net.sharksystem.app.messenger.commands.commandarguments;


import net.sharksystem.utils.Log;

/**
 * A question which can be asked and answered.
 */
public class UICommandQuestion {

    /**
     * The question
     */
    private final String question;

    /**
     * The argument in which the user input is stored
     */
    private final UICommandArgument<?> argument;

    public UICommandQuestion(String question, UICommandArgument<?> argument) {
        if(question == null || argument == null) Log.writeLogErr(this, "arguments must not be null");
        this.question = question;
        this.argument = argument;
    }

    public boolean submitAnswer(String input) throws Exception {
        return argument.tryParse(input);
    }

    public String getQuestionText() {
        return this.question;
    }
}
