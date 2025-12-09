package net.sharksystem.app.messenger.commands.helper;

import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandStringArgument;

import java.util.List;

public abstract class AbstractCommandWithSingleString extends UICommand {
    private final String defaultString;
    private final boolean optional;
    private String derString;
    private final UICommandStringArgument stringArgument;

    public AbstractCommandWithSingleString(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                           String identifier, boolean rememberCommand) {
        this(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand, false, null);
    }

    public AbstractCommandWithSingleString(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                           String identifier, boolean rememberCommand, boolean optional, String defaultString) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        this.stringArgument = new UICommandStringArgument(sharkMessengerApp);
        this.optional = optional;
        this.defaultString = defaultString;
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        this.derString = defaultString;
        if (arguments.size() < 1) {
            if(this.optional) return true;
            else this.getSharkMessengerApp().tellUI("string argument required");
            return false;
        }

        boolean isParsable = this.stringArgument.tryParse(arguments.get(0));
        if (!isParsable) {
            System.err.println("failed to parse string value" + arguments.get(0));
            return false;
        }

        return true;
    }

    protected String getStringArgument() {
        return this.stringArgument.getValue();
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
    }
}