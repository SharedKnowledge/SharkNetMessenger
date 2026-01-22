package net.sharksystem.ui.messenger.cli.commandarguments;

/**
 * This builder makes it a bit easier to create a questionnaire by saving some lines of code for creating needed objects.
 */
public class UICommandQuestionnaireBuilder {

    private final UICommandQuestionnaire questionnaire;

    public UICommandQuestionnaireBuilder() {
        this.questionnaire = new UICommandQuestionnaire();
    }

    /**
     * Adds a question to the questionnaire and returns itself, so new questions can be added directly
     * @param question the question as string
     * @param argument the argument which parse logic is used, and in which the parsed value is stored
     * @return itself
     */
    public UICommandQuestionnaireBuilder addQuestion(String question, UICommandArgument<?> argument) {
        this.questionnaire.addQuestion(new UICommandQuestion(question, argument));
        return this;
    }

    /**
     * Returns the questionnaire which was build
     */
    public UICommandQuestionnaire build() {
        return this.questionnaire;
    }
}
