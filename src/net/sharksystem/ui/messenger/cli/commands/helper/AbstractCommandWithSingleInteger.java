package net.sharksystem.ui.messenger.cli.commands.helper;

import net.sharksystem.ui.messenger.cli.commandarguments.UICommandIntegerArgument;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;

import java.util.List;

public abstract class AbstractCommandWithSingleInteger extends UICommand {
    private final boolean optional;
    private final int defaultValue;
    private int theValue;
    private final UICommandIntegerArgument integerArgument;

    public AbstractCommandWithSingleInteger(SharkNetMessengerApp sharkMessengerApp,
                                            SharkNetMessengerUI sharkMessengerUI, String identifier, boolean rememberCommand,
                                            boolean optional, int defaultValue) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        this.integerArgument = new UICommandIntegerArgument(sharkMessengerApp);
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.theValue = defaultValue;
    }

    public AbstractCommandWithSingleInteger(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                            String identifier, boolean rememberCommand) {
        this(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand, false, -1);
    }

    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            if(this.optional) return true;
            this.getSharkMessengerApp().tellUI("integer argument required");
            return false;
        } else {
            boolean isParsable = this.integerArgument.tryParse(arguments.get(0));
            if (!isParsable) {
                System.err.println("failed to parse integer value" + arguments.get(0));
            } else {
                this.theValue = this.integerArgument.getValue();
            }
            return isParsable;
        }
    }

    protected int getIntegerArgument() {
        return this.theValue;
    }

//    @Override
//    protected UICommandQuestionnaire specifyCommandStructure() {
//        // TODO Auto-generated method stub
//        throw new UnsupportedOperationException("Unimplemented method 'specifyCommandStructure'");
//    }
}