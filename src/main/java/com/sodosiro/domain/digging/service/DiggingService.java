package com.sodosiro.domain.digging.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.digging.controller.dto.request.DiggingCreateRequest;
import com.sodosiro.domain.digging.controller.dto.request.DiggingUpdateRequest;
import com.sodosiro.domain.digging.controller.dto.response.DiggingCandidateResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingListResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingResponse;
import com.sodosiro.domain.digging.entity.Digging;
import com.sodosiro.domain.digging.entity.DiggingBookmark;
import com.sodosiro.domain.digging.entity.DiggingImage;
import com.sodosiro.domain.digging.entity.DiggingLike;
import com.sodosiro.domain.digging.repository.DiggingBookmarkRepository;
import com.sodosiro.domain.digging.repository.DiggingImageRepository;
import com.sodosiro.domain.digging.repository.DiggingLikeRepository;
import com.sodosiro.domain.digging.repository.DiggingRepository;
import com.sodosiro.domain.digging.service.event.DiggingImageChangedEvent;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.DiggingErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.s3.constants.FileFolder;
import com.sodosiro.global.s3.service.S3Service;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DiggingService {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final long CURSOR_START = Long.MAX_VALUE;

    private final DiggingRepository diggingRepository;
    private final DiggingImageRepository diggingImageRepository;
    private final DiggingLikeRepository diggingLikeRepository;
    private final DiggingBookmarkRepository diggingBookmarkRepository;
    private final CourseRepository courseRepository;
    private final GpsRepository gpsRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    // ---------------- 후보 조회 ----------------

    /** 완료된 코스에서 GPS 인증된 여행지만 디깅 후보로 반환한다. */
    @Transactional(readOnly = true)
    public DiggingCandidateResponse getCandidates(Long userId, Long courseId) {
        Course course = findFinishedCourse(userId, courseId);

        Set<Long> verifiedContentIds = verifiedContentIds(courseId);
        Set<Long> postedContentIds = diggingRepository.findByCourseIdAndIsDeletedFalse(courseId).stream()
                .map(Digging::getContentId)
                .collect(Collectors.toSet());

        List<DiggingCandidateResponse.CandidateSpot> spots = course.getDays().stream()
                .flatMap(day -> day.spots().stream())
                .filter(spot -> verifiedContentIds.contains(spot.contentId()))
                // 여러 일자에 같은 스팟이 있을 수 있으므로 contentId 기준으로 중복 제거
                .collect(Collectors.toMap(
                        Course.SpotSnapshot::contentId,
                        Function.identity(),
                        (first, duplicate) -> first,
                        java.util.LinkedHashMap::new))
                .values().stream()
                .map(spot -> new DiggingCandidateResponse.CandidateSpot(
                        spot.contentId(),
                        spot.title(),
                        spot.firstImage(),
                        postedContentIds.contains(spot.contentId())))
                .toList();

        return new DiggingCandidateResponse(courseId, course.getStatus(), spots);
    }

    // ---------------- 작성 / 수정 / 삭제 ----------------

    @Transactional
    public DiggingResponse create(Long userId, DiggingCreateRequest request, List<MultipartFile> images) {
        validateImageCount(images == null ? 0 : images.size());

        findFinishedCourse(userId, request.courseId());

        if (!verifiedContentIds(request.courseId()).contains(request.contentId())) {
            throw new GeneralException(DiggingErrorCode._SPOT_NOT_VERIFIED);
        }
        if (diggingRepository.existsByCourseIdAndContentIdAndIsDeletedFalse(request.courseId(), request.contentId())) {
            throw new GeneralException(DiggingErrorCode._DIGGING_ALREADY_EXISTS);
        }

        List<String> uploadedUrls = uploadImages(images);
        eventPublisher.publishEvent(DiggingImageChangedEvent.onlyNew(uploadedUrls));

        Digging digging = diggingRepository.save(
                Digging.create(request.courseId(), request.contentId(), userId, request.body()));
        List<DiggingImage> savedImages = saveImages(digging.getId(), uploadedUrls);

        return DiggingResponse.of(digging, findUser(userId), findSpot(request.contentId()),
                savedImages, false, false, userId);
    }

    @Transactional
    public DiggingResponse update(
            Long userId, Long diggingId, DiggingUpdateRequest request, List<MultipartFile> images) {
        Digging digging = findOwnDigging(userId, diggingId);

        List<String> existingUrls = diggingImageRepository
                .findAllByDiggingIdOrderByDisplayOrderAsc(diggingId).stream()
                .map(DiggingImage::getImageUrl)
                .toList();

        List<String> keepUrls = request.keepImageUrls();
        if (!new HashSet<>(existingUrls).containsAll(keepUrls)) {
            throw new GeneralException(DiggingErrorCode._IMAGE_NOT_FOUND);
        }
        int newCount = images == null ? 0 : images.size();
        validateImageCount(keepUrls.size() + newCount);

        List<String> removedUrls = existingUrls.stream().filter(url -> !keepUrls.contains(url)).toList();
        List<String> newUrls = uploadImages(images);
        eventPublisher.publishEvent(DiggingImageChangedEvent.replace(newUrls, removedUrls));

        diggingImageRepository.deleteAllByDiggingId(diggingId);
        List<String> finalUrls = Stream.concat(keepUrls.stream(), newUrls.stream()).toList();
        List<DiggingImage> savedImages = saveImages(diggingId, finalUrls);

        digging.update(request.body());

        return DiggingResponse.of(digging, findUser(userId), findSpot(digging.getContentId()),
                savedImages, isLiked(userId, diggingId), isBookmarked(userId, diggingId), userId);
    }

    @Transactional
    public void delete(Long userId, Long diggingId) {
        Digging digging = findOwnDigging(userId, diggingId);

        List<String> imageUrls = diggingImageRepository
                .findAllByDiggingIdOrderByDisplayOrderAsc(diggingId).stream()
                .map(DiggingImage::getImageUrl)
                .toList();
        eventPublisher.publishEvent(DiggingImageChangedEvent.onlyOld(imageUrls));

        diggingImageRepository.deleteAllByDiggingId(diggingId);
        diggingLikeRepository.deleteAllByDiggingId(diggingId);
        diggingBookmarkRepository.deleteAllByDiggingId(diggingId);
        digging.delete();
    }

    // ---------------- 조회 ----------------

    @Transactional(readOnly = true)
    public DiggingResponse getOne(Long loginUserId, Long diggingId) {
        Digging digging = diggingRepository.findByIdAndIsDeletedFalse(diggingId)
                .orElseThrow(() -> new GeneralException(DiggingErrorCode._DIGGING_NOT_FOUND));

        return DiggingResponse.of(
                digging,
                userRepository.findById(digging.getUserId()).orElse(null),
                touristSpotRepository.findById(digging.getContentId()).orElse(null),
                diggingImageRepository.findAllByDiggingIdOrderByDisplayOrderAsc(diggingId),
                isLiked(loginUserId, diggingId),
                isBookmarked(loginUserId, diggingId),
                loginUserId);
    }

    @Transactional(readOnly = true)
    public DiggingListResponse getFeed(Long loginUserId, Long cursor, int size) {
        return toListResponse(diggingRepository.findFeed(effectiveCursor(cursor), size + 1), loginUserId, size);
    }

    @Transactional(readOnly = true)
    public DiggingListResponse getBySpot(Long loginUserId, Long contentId, Long cursor, int size) {
        return toListResponse(
                diggingRepository.findByContentId(contentId, effectiveCursor(cursor), size + 1), loginUserId, size);
    }

    @Transactional(readOnly = true)
    public DiggingListResponse getMine(Long userId, Long cursor, int size) {
        return toListResponse(
                diggingRepository.findByUserId(userId, effectiveCursor(cursor), size + 1), userId, size);
    }

    @Transactional(readOnly = true)
    public DiggingListResponse getMyBookmarks(Long userId, Long cursor, int size) {
        return toListResponse(
                diggingRepository.findBookmarkedByUserId(userId, effectiveCursor(cursor), size + 1), userId, size);
    }

    // ---------------- 내부 헬퍼 ----------------

    private DiggingListResponse toListResponse(List<Digging> fetched, Long loginUserId, int size) {
        boolean hasNext = fetched.size() > size;
        List<Digging> diggings = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext && !diggings.isEmpty() ? diggings.getLast().getId() : null;

        return new DiggingListResponse(assemble(diggings, loginUserId), nextCursor, hasNext);
    }

    /** N+1 을 피하기 위해 작성자·관광지·이미지·좋아요·즐겨찾기를 한 번씩만 조회해 조립한다. */
    private List<DiggingResponse> assemble(List<Digging> diggings, Long loginUserId) {
        if (diggings.isEmpty()) {
            return List.of();
        }
        List<Long> diggingIds = diggings.stream().map(Digging::getId).toList();

        Map<Long, User> userMap = userRepository
                .findAllById(diggings.stream().map(Digging::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        Map<Long, TouristSpot> spotMap = touristSpotRepository
                .findAllById(diggings.stream().map(Digging::getContentId).distinct().toList()).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        Map<Long, List<DiggingImage>> imageMap = diggingImageRepository
                .findAllByDiggingIdInOrderByDiggingIdAscDisplayOrderAsc(diggingIds).stream()
                .collect(Collectors.groupingBy(DiggingImage::getDiggingId));

        Set<Long> likedIds = loginUserId == null ? Set.of()
                : diggingLikeRepository.findByUserIdAndDiggingIdIn(loginUserId, diggingIds).stream()
                        .map(DiggingLike::getDiggingId).collect(Collectors.toSet());
        Set<Long> bookmarkedIds = loginUserId == null ? Set.of()
                : diggingBookmarkRepository.findByUserIdAndDiggingIdIn(loginUserId, diggingIds).stream()
                        .map(DiggingBookmark::getDiggingId).collect(Collectors.toSet());

        return diggings.stream()
                .map(digging -> DiggingResponse.of(
                        digging,
                        userMap.get(digging.getUserId()),
                        spotMap.get(digging.getContentId()),
                        imageMap.getOrDefault(digging.getId(), List.of()),
                        likedIds.contains(digging.getId()),
                        bookmarkedIds.contains(digging.getId()),
                        loginUserId))
                .toList();
    }

    /** 본인 소유이면서 status 가 FINISHED 인 코스만 통과시킨다. */
    private Course findFinishedCourse(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(DiggingErrorCode._COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.FINISHED) {
            throw new GeneralException(DiggingErrorCode._COURSE_NOT_FINISHED);
        }
        return course;
    }

    private Set<Long> verifiedContentIds(Long courseId) {
        return gpsRepository.findByCourseId(courseId).stream()
                .map(Gps::getContentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Digging findOwnDigging(Long userId, Long diggingId) {
        Digging digging = diggingRepository.findByIdAndIsDeletedFalse(diggingId)
                .orElseThrow(() -> new GeneralException(DiggingErrorCode._DIGGING_NOT_FOUND));
        if (!digging.getUserId().equals(userId)) {
            throw new GeneralException(DiggingErrorCode._DIGGING_FORBIDDEN);
        }
        return digging;
    }

    private boolean isLiked(Long userId, Long diggingId) {
        return userId != null && diggingLikeRepository.findByDiggingIdAndUserId(diggingId, userId).isPresent();
    }

    private boolean isBookmarked(Long userId, Long diggingId) {
        return userId != null && diggingBookmarkRepository.findByDiggingIdAndUserId(diggingId, userId).isPresent();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    private TouristSpot findSpot(Long contentId) {
        return touristSpotRepository.findById(contentId).orElse(null);
    }

    private long effectiveCursor(Long cursor) {
        return cursor == null ? CURSOR_START : cursor;
    }

    private void validateImageCount(int count) {
        if (count > MAX_IMAGE_COUNT) {
            throw new GeneralException(DiggingErrorCode._IMAGE_LIMIT_EXCEEDED);
        }
    }

    private List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return s3Service.uploadFiles(images, FileFolder.DIGGINGS);
    }

    private List<DiggingImage> saveImages(Long diggingId, List<String> urls) {
        if (urls.isEmpty()) {
            return List.of();
        }
        return diggingImageRepository.saveAll(IntStream.range(0, urls.size())
                .mapToObj(i -> DiggingImage.of(diggingId, urls.get(i), i))
                .toList());
    }
}
