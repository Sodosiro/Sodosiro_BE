package com.sodosiro.domain.user.service.event;

public record ProfileImageChangedEvent(String newUrl, String oldUrl) {

    public static ProfileImageChangedEvent updated(String newUrl, String oldUrl) {
        return new ProfileImageChangedEvent(newUrl, oldUrl);
    }

    public static ProfileImageChangedEvent removed(String oldUrl) {
        return new ProfileImageChangedEvent(null, oldUrl);
    }
}