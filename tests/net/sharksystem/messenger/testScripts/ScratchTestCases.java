package net.sharksystem.messenger.testScripts;

public class ScratchTestCases {
    private static final String BLOCK_RELEASE =
            "lsMessages;mkChannel snm://block_release;lsChannels;sendMessage A1 sn/characters 2;lsMessages 2";
    private static final String BLOCK_RELEASE_A =
            "openTCP 9999";
   private static final String BLOCK_RELEASE_B =
        "connectTCP localhost 9999";

    private static final String BLOCK_RELEASE_2_A =
            "openTCP 9999;block A1;markstep outA1";
    private static final String BLOCK_RELEASE_2_B =
            "connectTCP localhost 9999;lsMessages;mkChannel snm://block_release;lsChannels;wait 1000; sendMessage A1 sn/characters 2;lsMessages 2";

    private static final String BLOCK_RELEASE_3_A =
            "openTCP 9999;block A1;markstep outA1";
    private static final String BLOCK_RELEASE_3_B =
            "connectTCP localhost 9999;wait 100;release A1";

    private static final String BLOCK_RELEASE_4_A =
            "openTCP 9999;lsMessages;wait 5000;lsMessages 2";
    private static final String BLOCK_RELEASE_4_B =
            "connectTCP localhost 9999;release A1";
}
