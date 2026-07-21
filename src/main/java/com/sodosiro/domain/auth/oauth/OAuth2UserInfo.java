package com.sodosiro.domain.auth.oauth;


import lombok.Getter;

import java.util.Map;

@Getter
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }


    public abstract String getId();
    public abstract String getName();

    public abstract String getNameAttributeKey();

    public abstract String getEmail();

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}