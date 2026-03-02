package com.siontrack.siontrack.DTO.Request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayloadDTO {

    @Getter @Setter
    private List<Entry> entry;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        @Getter @Setter
        private List<Change> changes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        @Getter @Setter
        private Value value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        @Getter @Setter
        private List<Message> messages;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @Getter @Setter
        private String from;
        @Getter @Setter
        private String type;
        @Getter @Setter
        private TextContent text;
        @Getter @Setter
        private ButtonContent button;
        @Getter @Setter
        private InteractiveContent interactive;        
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextContent {
        @Getter @Setter
        private String body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ButtonContent {
        @Getter @Setter
        private String text;
        @Getter @Setter
        private String payload;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InteractiveContent {
        @Getter @Setter
        private String type;

        @JsonProperty("button_reply")
        @Getter @Setter
        private ButtonReply buttonReply;

        @JsonProperty("list_reply")
        @Getter @Setter
        private ListReply listReply;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ButtonReply {
        @Getter @Setter
        private String id;
        @Getter @Setter
        private String title;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListReply {
        @Getter @Setter
        private String id;
        @Getter @Setter
        private String title;
    }
}
