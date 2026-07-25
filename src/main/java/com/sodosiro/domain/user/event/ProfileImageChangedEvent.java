package com.sodosiro.domain.user.event;

public record ProfileImageChangedEvent(String newUrl, String oldUrl) {

}