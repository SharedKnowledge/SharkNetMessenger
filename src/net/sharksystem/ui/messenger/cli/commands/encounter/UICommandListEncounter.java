//package net.sharksystem.ui.messenger.cli.commands.encounter;
//
//import net.sharksystem.asap.ASAPEncounterManagerAdmin;
//import net.sharksystem.asap.utils.DateTimeHelper;
//import net.sharksystem.ui.messenger.cli.SharkNetMessengerApp;
//import net.sharksystem.ui.messenger.cli.SharkNetMessengerUI;
//import net.sharksystem.ui.messenger.cli.commands.helper.AbstractCommandNoParameter;
//
//import java.util.Iterator;
//
//public class UICommandListEncounter extends AbstractCommandNoParameter {
//    public UICommandListEncounter(SharkNetMessengerApp sharkMessengerApp, SharkNetMessengerUI sharkMessengerUI,
//                                  String identifier, boolean rememberCommand) {
//        super(sharkMessengerApp, sharkMessengerUI, identifier, rememberCommand);
//    }
//
//    @Override
//    protected void execute() throws Exception {
//        ASAPEncounterManagerAdmin encounterManagerAdmin = this.getSharkMessengerApp().getEncounterManagerAdmin();
//        Iterator<CharSequence> connectPeersIter = encounterManagerAdmin.getConnectedPeerIDs().iterator();
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("\n");
//        if(connectPeersIter == null || !connectPeersIter.hasNext()) {
//            sb.append("no open encounter in the moment\n");
//        } else {
//            int index = 1;
//            sb.append("open encounter\n");
//            while (connectPeersIter.hasNext()) {
//                sb.append(index);
//                sb.append(": ");
//                sb.append(connectPeersIter.next());
//                sb.append("\n");
//            }
//        }
//        sb.append("\n");
//        if(this.getSharkMessengerApp().getEncounterLogs().keySet().isEmpty()) {
//            sb.append("encounter list empty\n");
//        } else {
//            sb.append("encounter list:\n");
//            for(CharSequence peerID : this.getSharkMessengerApp().getEncounterLogs().keySet()) {
//                for (SharkNetMessengerApp.EncounterLog encounterLog :
//                        this.getSharkMessengerApp().getEncounterLogs().get(peerID)) {
//
//                    sb.append(encounterLog.toString());
//                }
//            }
//        }
//
//        sb.append("time before reconnect (in ms):\n");
//        sb.append(encounterManagerAdmin.getTimeBeforeReconnect());
//
//        sb.append("\n");
//        sb.append("deny list:\n");
//        sb.append(encounterManagerAdmin.getDenyList());
//
//        this.getSharkMessengerApp().tellUI(sb.toString());
//    }
//
//    @Override
//    public String getDescription() {
//        return "print open encounter";
//    }
//}
