package net.sharksystem.ui.messenger.cli.commands.messenger;

import java.util.List;

import net.sharksystem.ui.messenger.cli.commandarguments.UICommandChannelArgument;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaire;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.ui.messenger.cli.commandarguments.UICommandStringArgument;
import net.sharksystem.app.messenger.SharkNetCommunicationAge;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;

public class UICommandSetChannelAge extends UICommand {
    private final UICommandChannelArgument channel;
    private final UICommandStringArgument channelAge;

    public UICommandSetChannelAge(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                  String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.channel = new UICommandChannelArgument(sharkMessengerApp);
        channelAge = new UICommandStringArgument(sharkMessengerApp);
    }

    @Override
    public UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("Channel URI: ", this.channel)
                .addQuestion("Channel age: ", this.channelAge)
                .build();
    }

    @Override
    public void execute() throws Exception {
        SharkNetCommunicationAge age = SharkNetCommunicationAge.valueOf(this.channelAge.getValue());
        this.channel.getValue().setAge(age);
    }

    @Override
    public String getDescription() {
        return "Changes channel age.";
    }
    
    /**
     * @param arguments in following order:
     * <ol>
     *  <li>channel - channelURI</li>
     *  <li>channelAge - String</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if(arguments.size() < 2) {
            return false;
        }

        boolean isParsable = channel.tryParse(arguments.get(0)) 
                && channelAge.tryParse(arguments.get(1));

        return isParsable;
    }

}