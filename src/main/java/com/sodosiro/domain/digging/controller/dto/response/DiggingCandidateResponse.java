package com.sodosiro.domain.digging.controller.dto.response;

import com.sodosiro.domain.course.constants.CourseStatus;
import java.util.List;

public record DiggingCandidateResponse(
        Long courseId,
        CourseStatus courseStatus,
        List<CandidateSpot> spots
) {
    public record CandidateSpot(
            Long contentId,
            String title,
            String firstImage,
            boolean alreadyPosted
    ) {
    }
}
