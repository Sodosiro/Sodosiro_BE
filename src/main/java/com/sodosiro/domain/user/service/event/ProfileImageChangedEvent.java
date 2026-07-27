package com.sodosiro.domain.user.service.event;

public record ProfileImageChangedEvent(String newUrl, String oldUrl) {

}