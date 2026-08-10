package com.sodosiro.domain.course.controller.dto;

/** TouristSpot.category(1=식당, 2=카페, 3=쇼핑, 4=관광지, 5=자연, 6=액티비티) 중 숙박(7)을 제외한 여행스타일. */
public enum TravelStyle {
    RESTAURANT(1),
    CAFE(2),
    SHOPPING(3),
    TOURIST_SPOT(4),
    NATURE(5),
    ACTIVITY(6);

    private final int categoryCode;

    TravelStyle(int categoryCode) {
        this.categoryCode = categoryCode;
    }

    public int categoryCode() {
        return categoryCode;
    }
}
