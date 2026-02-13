package net.sharksystem.utils.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONParser {
    private JSONObject jsonDocument;

    private static String getContentAsString(File file) throws IOException {
        byte[] content = new byte[Math.toIntExact(file.length())];
        FileInputStream fis = new FileInputStream(file);
        fis.read(content);

        // expect a json UTF-8 string
        return new String(content);
    }

    public JSONParser(File file) throws IOException {
        this(getContentAsString(file));
    }

    public JSONParser(String jsonString) throws IOException {
        this.jsonDocument = this.parseFullJSONString(jsonString);
    }

    public JSONObject getParsedDocument() throws IOException {
        return this.jsonDocument;
    }

    /////////////////////////////// inside object parsing
    private int[] getNextIndexesOfQuotes (String jsonString,int startIndex) throws IOException {
        int[] indexes = new int[2];
        indexes[0] = jsonString.indexOf("\"", startIndex);
        if (indexes[0] < 0) throw new IOException("no quote sign found - malformed json format");
        indexes[1] = jsonString.indexOf("\"", indexes[0] + 1);
        if (indexes[1] < 0) throw new IOException("no quote sign found - malformed json format");
        return indexes;
    }

    private String[] getKeyAndRestString(String jsonString) throws IOException {
        int[] indexes = getNextIndexesOfQuotes(jsonString, 0);

        String[] parameterAndRest = new String[2];
        parameterAndRest[0] = jsonString.substring(indexes[0]+1, indexes[1]);

        // skip ":"
        int indexDoublePoint = jsonString.indexOf(":", indexes[1]);
        if(indexDoublePoint == -1)
            throw new IOException("no double point after key found - malformed json: " + jsonString);

        parameterAndRest[1] = jsonString.substring(indexDoublePoint);

        return parameterAndRest;
    }

    private JSONObject parseFullJSONString(String jsonSubstring) throws IOException {
        int firstIndex = jsonSubstring.indexOf("{");
        if(firstIndex == -1) throw new IOException("no brace found at all - malformed json");
        int lastIndex = jsonSubstring.lastIndexOf("}");
        if(lastIndex == -1) throw new IOException("no closing brace found - malformed json");

        // cut head and tail
        jsonSubstring = jsonSubstring.substring(firstIndex+1, lastIndex);

        // start parsing
        return this.parseToken(jsonSubstring, JSONValueType.KV_SET);
    }

    private int findCorrespondingClosingSign(String openingSign, String closingSign, String jsonString) throws IOException {
        // Note: opening sign is still in the string - step over before begin
        int currentIndex = jsonString.indexOf(openingSign)+1;
        int depth = 0; // we are on level 0
        while(true) {
            int indexOpenSign = jsonString.indexOf(openingSign, currentIndex);
            int indexClosingSign = jsonString.indexOf(closingSign, currentIndex);

            /* what can happen
                a) openS...closeS -> go deeper
                b) closeS... -> go up: if 0, found it
                c) closeS not found - failure
             */
            // c)
            if (indexClosingSign == -1)
                throw new IOException("no closing sign found - malformed json: " + closingSign + " | in: " + jsonString);
            // closingSign found - so far so good.

            // a) enter a deeper level?
            if(indexOpenSign != -1 && indexOpenSign < indexClosingSign) {
                depth++;
                currentIndex = indexOpenSign+1; // maybe we go even deeper
            }

            // b) go up
            else if(indexOpenSign == -1 || indexOpenSign > indexClosingSign) {
                if(depth == 0) return indexClosingSign; // found it
                depth--;
                currentIndex = indexClosingSign+1; // escape that level
            }
        }
    }

    private JSONValueType whatsNext(String jsonSubstring) {
        int indexOpenBrace = jsonSubstring.indexOf("{");
        int indexOpenSquareBracket = jsonSubstring.indexOf("[");
        int indexQuote = jsonSubstring.indexOf("\"");

        // sub object?
        if (indexOpenBrace != -1
                && (indexOpenSquareBracket == -1 || indexOpenBrace < indexOpenSquareBracket)
                && (indexQuote == -1 || indexOpenBrace < indexQuote)) {
            return JSONValueType.KV_SET;
        }
        if (indexOpenSquareBracket != -1
                && (indexOpenBrace == -1 || indexOpenSquareBracket < indexOpenBrace)
                && (indexQuote == -1 || indexOpenSquareBracket < indexQuote)) {
            return JSONValueType.KV_LIST;
        }
        if (indexQuote != -1
                && (indexOpenSquareBracket == -1 || indexQuote < indexOpenSquareBracket)
                && (indexOpenBrace == -1 || indexQuote < indexOpenBrace)) {
            return JSONValueType.STRING_VALUE;
        }
        return JSONValueType.UNKNOWN;
    }

    private JSONObject parseToken(String jsonSubstring, JSONValueType type)
            throws IOException {
        /*
        token :- token "," token
        token :- "quota" key "quota" ":" value
        value :- object, list, string
        object :- "{" token "}"
        list :- "[" token "]"
        string :- "quota" value "quota"
        quota :- "\""
         */

        // what are we filling now?
        JSONObject currentJsonObject = new JSONObject(type);

        int currentIndex = 0;
        do {
            // find key - next string between ""
            String[] parameterAndRest = null;
            try {
                parameterAndRest = this.getKeyAndRestString(jsonSubstring);
            } catch (IOException e) {
                // reached end
                return currentJsonObject; // TODO
            }
            String key = parameterAndRest[0];

            // forget what we read
            jsonSubstring = parameterAndRest[1];
            currentIndex = 0;

            int indexOpeningTag, indexClosingTag;
            JSONValueType nextType = this.whatsNext(jsonSubstring);
            String openingSign = null, closingSign = null;

            switch (nextType) {
                case JSONValueType.KV_SET:
                    openingSign = "{";
                    closingSign = "}";
                    break;
                case JSONValueType.KV_LIST:
                    openingSign = "[";
                    closingSign = "]";
                    break;
                case JSONValueType.STRING_VALUE:
                    openingSign = "\"";
                    closingSign = "\"";
                    break;
                default:
                    throw new IOException("unknown json object ahead - malformed json string: " + jsonSubstring);
            }

            switch (nextType) {
                case JSONValueType.KV_SET:
                case JSONValueType.KV_LIST:
                    // find opening brace
                    indexOpeningTag = jsonSubstring.indexOf(openingSign, currentIndex);
                    // find closing brace
                    indexClosingTag = this.findCorrespondingClosingSign(openingSign, closingSign, jsonSubstring);
                    break;

                case JSONValueType.STRING_VALUE:
                    indexOpeningTag = jsonSubstring.indexOf(openingSign, currentIndex);
                    indexClosingTag = jsonSubstring.indexOf(closingSign, indexOpeningTag + 1);
                    break;

                default:
                    throw new IOException("unknown json object ahead - malformed json string: " + jsonSubstring);
            }

            String tokenString = jsonSubstring.substring(indexOpeningTag + 1, indexClosingTag);

            // forget what we read and go ahead for further processing
            jsonSubstring = jsonSubstring.substring(indexClosingTag);
            currentIndex = jsonSubstring.indexOf(",");
            currentIndex++; // works anyway -1 -> 0; otherwise: behind comma
            jsonSubstring = jsonSubstring.substring(currentIndex);
            currentIndex = 0;

            // process token
            switch (nextType) {
                case JSONValueType.KV_SET:
                case JSONValueType.KV_LIST:
                    JSONObject jsonObject = this.parseToken(tokenString, nextType);
                    currentJsonObject.addElement(key, jsonObject);
                    break;

                case JSONValueType.STRING_VALUE:
                    currentJsonObject.addElement(key, new JSONObject(tokenString));
                    break;
            }
        } while (jsonSubstring != null && currentIndex != -1);

    return currentJsonObject;
    }
}
