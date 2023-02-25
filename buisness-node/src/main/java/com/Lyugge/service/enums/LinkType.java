package com.Lyugge.service.enums;

public enum LinkType {
    GET_DOC("get_doc"),
    GET_PHOTO("get_photo");
    private final String link;

    LinkType(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return link;
    }
}
