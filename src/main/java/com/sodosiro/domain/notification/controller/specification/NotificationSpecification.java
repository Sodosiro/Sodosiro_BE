package com.sodosiro.domain.notification.controller.specification;

import com.sodosiro.domain.notification.controller.dto.NotificationListResponse;
import com.sodosiro.domain.notification.controller.dto.NotificationPreferenceResponse;
import com.sodosiro.domain.notification.controller.dto.NotificationPreferenceToggleRequest;
import com.sodosiro.domain.notification.controller.dto.PushTokenUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "알림", description = "알림 목록·읽음 처리, 기기 푸시 토큰, 푸시 수신 설정 API")
public interface NotificationSpecification {

    @Operation(summary = "알림 목록 조회",
            description = "로그인 사용자의 알림을 최신순(id 내림차순)으로 조회합니다. "
                    + "cursor 는 직전 응답의 nextCursor 를 그대로 전달하며, 첫 페이지에서는 생략합니다. "
                    + "응답의 unreadCount 는 커서와 무관하게 사용자의 전체 안 읽은 알림 수입니다. "
                    + "size 는 1~100 범위이며 벗어나면 400을 반환합니다. "
                    + "payload 형식은 type 마다 다르며 아래 예시로 타입별 실제 구조를 확인할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(
            mediaType = "application/json",
            examples = {
                    @ExampleObject(name = "NEARBY_LIKED_SPOTS", summary = "찜한 장소 근처 진입", value = """
                            {
                              "items": [
                                {
                                  "id": 101,
                                  "type": "NEARBY_LIKED_SPOTS",
                                  "title": "찜한 장소 4곳이 근처에 있어요",
                                  "body": "설악산 국립공원, 낙산사, 속초해수욕장, 아바이마을",
                                  "payload": {
                                    "courseId": 12,
                                    "nearbyContentIds": [126508, 126510, 126522, 126599],
                                    "nearbyCount": 4
                                  },
                                  "isRead": false,
                                  "createdAt": "2026-08-23T14:30:00"
                                }
                              ],
                              "nextCursor": "101",
                              "hasNext": false,
                              "unreadCount": 1
                            }
                            """),
                    @ExampleObject(name = "REVIEW_REQUEST", summary = "리뷰 작성 요청", value = """
                            {
                              "items": [
                                {
                                  "id": 98,
                                  "type": "REVIEW_REQUEST",
                                  "title": "속초 여행은 어떠셨나요?",
                                  "body": "다녀오신 곳의 리뷰를 남겨주세요. 다른 여행자에게 큰 도움이 돼요!",
                                  "payload": {
                                    "courseId": 12,
                                    "pendingSpotCount": 3
                                  },
                                  "isRead": true,
                                  "createdAt": "2026-08-20T09:00:00"
                                }
                              ],
                              "nextCursor": null,
                              "hasNext": false,
                              "unreadCount": 0
                            }
                            """),
                    @ExampleObject(name = "DIGGING_POST_LIKE", summary = "내 디깅에 좋아요", value = """
                            {
                              "items": [
                                {
                                  "id": 102,
                                  "type": "DIGGING_POST_LIKE",
                                  "title": "내 게시물에 좋아요 2개가 달렸어요",
                                  "body": "영진횟집",
                                  "payload": {
                                    "diggingId": 3,
                                    "likerUserId": 100
                                  },
                                  "isRead": false,
                                  "createdAt": "2026-08-23T15:10:00"
                                }
                              ],
                              "nextCursor": null,
                              "hasNext": false,
                              "unreadCount": 2
                            }
                            """)
            }
    ))
    ResponseEntity<NotificationListResponse> list(Long userId, String cursor, int size);

    @Operation(summary = "알림 단건 읽음 처리",
            description = "본인의 알림 1건을 읽음으로 표시합니다. 이미 읽은 알림이면 상태와 읽은 시각을 그대로 유지합니다(멱등). "
                    + "본인 알림이 아니거나 존재하지 않으면 404를 반환합니다.")
    ResponseEntity<Void> read(Long userId, Long notificationId);

    @Operation(summary = "알림 전체 읽음 처리",
            description = "아직 읽지 않은 알림을 모두 읽음으로 표시합니다. 읽지 않은 알림이 없어도 성공(204)입니다.")
    ResponseEntity<Void> readAll(Long userId);

    @Operation(summary = "기기 푸시 토큰 등록/갱신",
            description = "FCM 등록 토큰을 사용자-기기 단위로 저장합니다. 같은 deviceId 로 다시 호출하면 토큰을 갱신합니다. "
                    + "다른 사용자가 쓰던 토큰이 전달되면 기존 소유자에게서 토큰을 분리한 뒤 등록합니다(기기 재로그인 대응). "
                    + "앱 최초 실행·로그인 직후, 그리고 onNewToken 으로 토큰이 갱신될 때 호출하세요.")
    ResponseEntity<Void> upsertToken(Long userId, String deviceId, PushTokenUpsertRequest request);

    @Operation(summary = "기기 푸시 해제",
            description = "해당 기기를 비활성화해 더 이상 푸시를 보내지 않습니다(로그아웃 시 호출). "
                    + "이미 비활성화됐거나 존재하지 않는 기기여도 성공(204)입니다.")
    ResponseEntity<Void> deleteDevice(Long userId, String deviceId);

    @Operation(summary = "알림 수신 설정 조회",
            description = "로그인 사용자의 알림 종류별 푸시(FCM) 수신 여부를 조회합니다. 한 번도 설정한 적 없으면 전체 기본값(true)을 반환합니다. "
                    + "이 설정을 꺼도 앱 내 알림 목록(GET /notifications)에는 계속 쌓이며, FCM 푸시 전송만 막힙니다. "
                    + "allEnabled(전체 알림)가 꺼져 있으면 나머지 타입별 설정과 무관하게 모든 푸시가 전송되지 않습니다.")
    ResponseEntity<NotificationPreferenceResponse> getPreference(Long userId);

    @Operation(summary = "알림 수신 설정 토글",
            description = "type 하나에 대해 알림 수신 여부를 켜거나 끕니다. "
                    + "type은 ALL(전체 알림), NEARBY_LIKED_SPOTS(여행 알림 · 찜한 장소 근처 진입), "
                    + "REVIEW_REQUEST(여행 리뷰 리마인드), DIGGING_POST_LIKE(디깅 좋아요 알림) 중 하나입니다. "
                    + "꺼도 앱 내 알림 목록 적재는 계속되고, 이후 발생하는 해당 타입 알림의 FCM 푸시 전송만 건너뜁니다. "
                    + "응답은 토글 이후의 전체 설정 상태입니다.")
    ResponseEntity<NotificationPreferenceResponse> togglePreference(Long userId, NotificationPreferenceToggleRequest request);
}
