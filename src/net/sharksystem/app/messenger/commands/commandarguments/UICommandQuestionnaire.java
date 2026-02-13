package net.sharksystem.app.messenger.commands.commandarguments;

import java.util.ArrayList;
import java.util.List;

/**
 * A questionnaire storing questions in the order in which the questions were added.
 */
public class UICommandQuestionnaire {

    /**
     * What the user needs to input in order to end the questionnaire
     */
    public static final String EXIT_SEQUENCE = "exit";

    private final List<UICommandQuestion> questionnaire;

    public UICommandQuestionnaire() {
        this.questionnaire = new ArrayList<>();
    }

    public void addQuestion(UICommandQuestion question) {
        this.questionnaire.add(question);
    }

    public List<UICommandQuestion> getQuestions() {
        return this.questionnaire;
    }
}
