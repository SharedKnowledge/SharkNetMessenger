package net.sharksystem.messenger.testScripts;

import net.sharksystem.utils.json.JSONObject;
import net.sharksystem.utils.json.JSONParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class OrchestratedTestTests {
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                       block / release implementation tests                                   //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String BLOCK_RELEASE =
            "lsMessages;mkChannel snm://block_release;lsChannels;sendMessage A1 sn/characters 2;lsMessages 2";
    private static final String BLOCK_RELEASE_A =
            "openTCP 9999";
   private static final String BLOCK_RELEASE_B =
        "connectTCP localhost 9999";

    private static final String BLOCK_RELEASE_2_A =
            "openTCP 9999;block A1;markstep outA1";
    private static final String BLOCK_RELEASE_2_B =
            "connectTCP localhost 9999;lsMessages;mkChannel snm://block_release;" +
                    "lsChannels;wait 1000; sendMessage A1 sn/characters 2;lsMessages 2";

    private static final String BLOCK_RELEASE_3_A =
            "openTCP 9999;block A1;markstep outA1";
    private static final String BLOCK_RELEASE_3_B =
            "connectTCP localhost 9999;wait 100;release A1";

    private static final String BLOCK_RELEASE_4_A =
            "openTCP 9999;lsMessages;wait 5000;lsMessages 2";
    private static final String BLOCK_RELEASE_4_B =
            "connectTCP localhost 9999;release A1";

    @Test
    public void jsonParserTest() throws IOException {
//        File jsonFile = new File("jsonTests_3.txt");
        File jsonFile = new File("jsonTests.txt");
        /*
        String jsonString = "{\"parameter\": \"value\", \"p2\": \"v2\"}";
        FileOutputStream fos = new FileOutputStream(jsonFile);
        fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
         */

        JSONParser jsonParser = new JSONParser(jsonFile);
        JSONObject parsedDocument = jsonParser.getParsedDocument();

        int debuggerBreak = 42;

        ///// test files
        /*
1)
{
"o2":
	{ "parameter": "value", "p2": "v2",
		"sub2: { "key1": "v1", "k2": "v2"}
	}
"o1":
	{"parameter": "value", "p2": "v2"}

}
2)
{
"0": {
	"1": {
		"2": { "k1": "v1", "k2": "v2"}
		}
	}
}
3)
{
"0": [ "k1": "v1", "k2": "v2", "k3": "v3"]
}
4)
{
"k1": "v1",
"k2": "v2",
"k3": "v3",
}
         */
    }
}
