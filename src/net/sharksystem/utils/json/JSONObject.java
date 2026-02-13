package net.sharksystem.utils.json;

import java.io.IOException;
import java.util.*;

/**
 * A json object is a key/value pair
 * A json value is a set of key / json value pairs, a list of key / json value pairs or a string
 */
public class JSONObject {
    private Map<String, JSONObject> set;
    private List<JsonKVPair> list;
    private String value;
    private JSONValueType type;

    JSONObject(JSONValueType type) {
        switch (type) {
            case KV_SET:
                this.set = new HashMap<>();
                break;
            case KV_LIST:
                this.list = new ArrayList<>();
                break;
        }
        this.type = type;
    }

    JSONObject(String value) {
        this.type = JSONValueType.STRING_VALUE;
        this.value = value;
    }

    private JSONObject(String key, JSONObject value) {
        this.type = JSONValueType.KV_SET;
        this.set = new HashMap<>();
        this.set.put(key, value);
    }

    public JSONObject getValue(String key) throws IOException {
        JSONObject jsonObject = null;
        if(this.set != null) {
             jsonObject = this.set.get(key);
        }
        else if(this.list != null) {
            for(JsonKVPair kvPair : this.list) {
                if(kvPair.key.equalsIgnoreCase(key)) {
                    jsonObject = kvPair.value;
                }
            }
        }

        if(jsonObject == null) throw new IOException("unknown key: " + key);
        return jsonObject;
    }

    public String getValue() throws IOException {
        if(this.value == null) throw new IOException("no value found");
        return this.value;
    }

    public JSONValueType getType() {
        return this.type;
    }

    public void addElement(String key, JSONObject value) {
        if (this.set != null) this.set.put(key, value);
        else this.list.add(new JsonKVPair(key, value));
    }

    public List<JSONObject> getObjectsList() throws IOException {
        if(this.list == null || this.list.isEmpty()) throw new IOException("no object list");
        List<JSONObject> objectsList = new ArrayList<>();

        for(JsonKVPair kvEntry : this.list) {
            objectsList.add(kvEntry.value);
        }

        return objectsList;
    }

    public List<String> getStringValueList() throws IOException {
        List<String> stringList = new ArrayList<>();

        if(this.list != null) {
            for(JsonKVPair kvPair : this.list) {
                if(kvPair.value.type != JSONValueType.STRING_VALUE)
                    throw new IOException("object contains not only string parameter");

                stringList.add(kvPair.value.value);
            }
        }
        else if(this.set != null) {
            for( JSONObject value :this.set.values()) {
                if(value.type != JSONValueType.STRING_VALUE)
                    throw new IOException("object contains not only string parameter");

                stringList.add(value.value);
            }
        }

        return stringList;
    }

    public Set<String> getKeys() throws IOException {
        if(this.set == null) throw new IOException("object does have key/value pairs");
        return this.set.keySet();
    }

    public class JsonKVPair {
        public final String key;
        public final JSONObject value;

        public JsonKVPair(String key, JSONObject value) {
            this.key = key;
            this.value = value;
        }
    }
}
