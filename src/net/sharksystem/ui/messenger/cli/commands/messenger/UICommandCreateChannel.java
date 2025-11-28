package net.sharksystem.ui.messenger.cli.commands.messenger;

import net.sharksystem.SharkException;
import net.sharksystem.app.messenger.SharkNetMessengerComponent;
import net.sharksystem.app.messenger.SharkNetMessengerException;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;

import java.io.IOException;
import java.util.List;

/**
 * Create a channel for writing messages to it.
 */
public class UICommandCreateChannel extends UICommand {
    private final UICommandStringArgument channelUriArgument;
    private final UICommandStringArgument channelNameArgument;

    private String channelUri = null;
    private String channelName = null;

    public UICommandCreateChannel(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                  String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);

        this.channelUriArgument = new UICommandStringArgument(sharkMessengerApp);
        this.channelNameArgument = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("Please input the channel uri: ", this.channelUriArgument)
                .addQuestion("Please set the channel name: ", this.channelNameArgument)
                .build();
    }

    @Override
    public void execute() {
        String channelURI = this.channelUriArgument.getValue();
        String channelName = this.channelNameArgument.getValue();

        try {
            SharkNetMessengerComponent sharkMessengerComponent = this.getSharkMessengerApp().getSharkMessengerComponent();
            try {
                sharkMessengerComponent.getChannel(channelURI);
                this.getSharkMessengerApp().tellUI("nothing to do; channel already exists: " + channelURI);
            }
            catch (SharkException se) {
                sharkMessengerComponent.createChannel(channelURI, channelName, true);
            }
        } catch (SharkNetMessengerException | IOException e) {
            this.printErrorMessage(e.getLocalizedMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Creates a new channel by uri.";
    }
    
    /**
     * @param arguments in following order:
     * <ol>
     *  <li>channelUri - String</li>
     *  <li>channelName - String</li>
     *  <li>channelMustNotExist - boolean</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 1) {
            this.getSharkMessengerApp().
                    tellUIError("\nrequire: at least channel_uri; optional channel_name age [s(tone),b(ronze),i(ron)]");
            return false;
        }
        boolean isParsable = true;
        if(!channelUriArgument.tryParse(arguments.get(0))) {
            this.getSharkMessengerApp().tellUIError("no valid uri: " + arguments.get(0));
            isParsable = false;
        } else {
            this.channelUri = this.channelUriArgument.getValue();
        }

        if(arguments.size()> 1) {
            if(!channelNameArgument.tryParse(arguments.get(1))) {
                this.getSharkMessengerApp().tellUI("no channel name, take default (none).");
            }
        } else {
            this.channelName = this.channelNameArgument.getValue();
        }

        if(arguments.size()> 2) {
            // TODO parse channel age
        }

        this.getSharkMessengerApp().
                tellUI("info: channel communication era cannot be defined in this version. Always set Internet age.");

        return isParsable;
    }
}
