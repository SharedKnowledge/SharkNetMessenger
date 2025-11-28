package net.sharksystem.ui.messenger.cli.testtools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestLanguageCompiler {
    public static final char CONNECT_COMMAND_SIGN = 'C';
    public static final char SEPARATOR = ';';

    public Map<String, String> compile(String sentence) {
        /**
         * sentence -> command SEPARATOR command
         * command -> connectCommand | ...
         * connectCommand -> CONNECT_COMMAND_SIGN  peer peer
         * peer -> 'a', 'b'
         */
        Map<String, StringBuilder> peerScripts = new HashMap<>();
        StringBuilder sb = new StringBuilder();

        // split sentence into commands
        int i = sentence.indexOf(SEPARATOR);
        List<String> commandStrings = new ArrayList<>();
        do {
            if(i == -1)  {
                // last command
                commandStrings.add(sentence);
            } else {
                commandStrings.add(sentence.substring(0,i));
                sentence = sentence.substring(i);
            }
        } while(i > -1);

        // done, finalise scripts
        Map<String, String> scripts = new HashMap<>();
        for(String peerName : peerScripts.keySet()) {
            scripts.put(peerName, peerScripts.get(peerName).toString());
        }

        return scripts;
    }
}
