package net.sharksystem.messenger.testScripts;

import java.util.HashMap;
import java.util.Map;

public abstract class PKITestcases {
    // testname and names of involved parties
    private static Map<String, String[]> version1Tests = new HashMap();
    {
        version1Tests.put("PKI1", new String[]{"A","B","C"});
    }

    // testcase: Cab; Bob certifies Alice; Alice sends a message; close; B meets Clara;
    // Clara receives message but cannot verify
    String PKI1_A = "openTCP 9999;wait 11000;sendCredential;W;sendMessage HIItsAlice;W;exit";
    String PKI1_B = "wait 10000; connectTCP localhost 9999;W; acceptCredential 1;lsMessages;" +
            "connectTCP localhost 8888;sendCredential";
    String PKI1_C = "W:tillAlicestops;openTCP 8888;lsMessages;lsCerts;comment:Clara has Cert, " +
            "kann aber nicht verifizieren;acceptCredential 1;lsMessages";

}
