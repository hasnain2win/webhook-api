package com.hasnain.webhook_api.utils;


import java.util.Map;

public class ChannelNameGenerator {

    public static String generateChannelName(Map<String, Object> eventPayload) {

        if(eventPayload!=null) {
            String creatorName = (String) ((Map<String, Object>) eventPayload.get("pusher")).get("name");
            String commitId = (String) eventPayload.get("after"); // or from "head_commit.id"

            return String.format("creator:%s:commit:%s", creatorName, commitId);
        }
        return null;
    }

}
