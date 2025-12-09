package net.sharksystem.app.messenger.commands.hubaccess;

import java.util.List;

import net.sharksystem.app.messenger.commands.commandarguments.UICommandIntegerArgument;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaire;
import net.sharksystem.app.messenger.commands.commandarguments.UICommandQuestionnaireBuilder;
import net.sharksystem.hub.peerside.HubConnectorDescription;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
import net.sharksystem.ui.messenger.cli.UICommand;

public class UICommandRemoveHubDescription extends UICommand {
    private final UICommandIntegerArgument hubIndex;

    public UICommandRemoveHubDescription(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
                                         String identifier, boolean rememberCommand) {
        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
        this.hubIndex = new UICommandIntegerArgument(sharkMessengerApp);
    }

    @Override
    protected UICommandQuestionnaire specifyCommandStructure() {
        return new UICommandQuestionnaireBuilder()
                .addQuestion("index in hub list (produced by lsHubs)? ", this.hubIndex)
                .build();
    }

    @Override
    protected void execute() throws Exception {
        HubConnectorDescription hubDescription =
                this.getSharkMessengerApp().getSharkPeer().getHubDescription(this.hubIndex.getValue());

        this.getSharkMessengerUI().getOutStream().println("remove hub description");
        HubDescriptionPrinter.print(this.getSharkMessengerUI().getOutStream(), hubDescription);

        this.getSharkMessengerApp().getSharkPeer().removeHubDescription(hubDescription);
    }

    @Override
    public String getDescription() {
        return "Remove hub description from list.";
    }

    /**
     * @param arguments in following order:
     * <ol>
     *  <li>hubIndex - int</li>
     * </ol>
     */
    @Override
    protected boolean handleArguments(List<String> arguments) {
        if (arguments.size() < 1) {
            return false;
        }

        boolean isParsable = hubIndex.tryParse(arguments.get(0));

        return isParsable;
    }
}
