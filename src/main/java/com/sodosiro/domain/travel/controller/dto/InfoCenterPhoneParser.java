package com.sodosiro.domain.travel.controller.dto;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InfoCenterPhoneParser {

    private static final Pattern PHONE_NUMBER = Pattern.compile(
            "(?<!\\d)(?:0\\d{1,2}-\\d{3,4}-\\d{4}|1\\d{3}-\\d{4})(?!\\d)");

    private InfoCenterPhoneParser() {
    }

    static String extract(String infoCenter) {
        if (infoCenter == null || infoCenter.isBlank()) {
            return null;
        }

        Matcher matcher = PHONE_NUMBER.matcher(infoCenter);
        LinkedHashSet<String> phoneNumbers = new LinkedHashSet<>();
        while (matcher.find()) {
            phoneNumbers.add(matcher.group());
        }
        return phoneNumbers.isEmpty() ? null : String.join("/", phoneNumbers);
    }
}
