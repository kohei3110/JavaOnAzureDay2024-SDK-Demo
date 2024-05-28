package com.koheisaito.javaonazureday2024.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestBodySpeech {

    @JsonProperty("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}