package com.example.travel.domain.travel.ai;

import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.model.TravelRecommendationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TravelAiPromptService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonNode RESPONSE_SCHEMA = readSchema();

    public String instructions() {
        return """
                너는 전라도 여행 일정을 구성하는 AI 큐레이터다.
                반드시 입력의 candidates에 포함된 관광지만 사용한다.
                contentId는 입력값을 한 글자도 변경하지 않는다.
                주소, 좌표, 운영시간, 가격, 교통정보를 만들거나 추측하지 않는다.
                동일한 contentId를 중복 선택하지 않는다.
                실제 이동시간은 백엔드가 계산하므로 작성하지 않는다.
                recommendedTime은 반드시 24시간제 HH:mm 형식으로 작성한다. 예: 09:30, 14:00.
                '오전', '오후', '아침', '점심', '저녁' 같은 표현은 사용하지 않는다.
                travelTip은 반드시 자연스러운 한국어 문장으로만 작성한다.
                고유명사와 불가피한 약어를 제외하고 영어 문장으로 작성하지 않는다.
                사용자의 날짜, 테마, 이동 방식, 동행 유형과 후보 점수를 고려한다.
                dailyPlaceCounts는 1일차부터 날짜 순서대로 배치할 관광지 개수다.
                각 날짜에는 dailyPlaceCounts에 지정된 개수와 정확히 같은 수의 장소를 배치한다.
                candidates의 latitude와 longitude, firstDayStartLocation, lodgingLocation,
                lastDayEndLocation의 좌표를 사용해 날짜별 장소와 방문 순서를 구성한다.
                첫날은 firstDayStartLocation에서 출발해 관광지를 방문한 뒤 lodgingLocation에서 끝난다.
                중간 날짜는 lodgingLocation에서 출발해 관광지를 방문한 뒤 lodgingLocation으로 돌아온다.
                마지막 날은 lodgingLocation에서 출발해 관광지를 방문한 뒤 lastDayEndLocation에서 끝난다.
                여행이 하루뿐이면 firstDayStartLocation에서 출발해 관광지를 방문한 뒤
                lastDayEndLocation에서 끝난다.
                각 날짜의 시작점에서 종료점으로 자연스럽게 이동하도록 가까운 장소를 연속 배치하고,
                직선거리상 불필요한 왕복, 같은 방향의 반복 이동, 이미 지나온 지역으로의 복귀를 최소화한다.
                좌표는 장소 배치와 순서 결정에만 사용하며 실제 도로 거리나 이동시간을 추측하지 않는다.
                자연스러운 추천 이유를 한국어로 작성한다.
                """;
    }

    public String input(TravelRecommendationContext request,
                        List<TravelCandidateItem> candidates) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("region", request.getRegionName());
        input.put("startDate", request.getStartsOn().toString());
        input.put("endDate", request.getEndsOn().toString());
        input.put("tripDays", ChronoUnit.DAYS.between(
                request.getStartsOn(), request.getEndsOn()) + 1);
        input.put("lodgingLocation", Map.of(
                "name", request.getLodgingName(),
                "address", request.getLodgingAddress(),
                "latitude", request.getLodgingLatitude(),
                "longitude", request.getLodgingLongitude()));
        input.put("firstDayStartLocation", Map.of(
                "name", request.getStartPlaceName(),
                "address", request.getStartPlaceAddress(),
                "latitude", request.getStartLatitude(),
                "longitude", request.getStartLongitude()));
        input.put("lastDayEndLocation", Map.of(
                "name", request.getEndPlaceName(),
                "address", request.getEndPlaceAddress(),
                "latitude", request.getEndLatitude(),
                "longitude", request.getEndLongitude()));
        input.put("themes", List.of(request.getThemes()));
        input.put("transportType", request.getTransportType().name());
        input.put("companionType", request.getCompanionType().name());
        input.put("dailyPlaceCounts", List.of(request.getDailyPlaceCounts()));
        input.put("candidates", candidates);
        try {
            return OBJECT_MAPPER.writeValueAsString(input);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("여행 추천 프롬프트 생성에 실패했습니다.", exception);
        }
    }

    public JsonNode responseSchema() {
        return RESPONSE_SCHEMA;
    }

    private static JsonNode readSchema() {
        try {
            return OBJECT_MAPPER.readTree("""
                    {
                      "type": "object",
                      "additionalProperties": false,
                      "required": ["title", "summary", "days", "travelTip"],
                      "properties": {
                        "title": {"type": "string"},
                        "summary": {"type": "string"},
                        "travelTip": {"type": "string"},
                        "days": {
                          "type": "array",
                          "items": {
                            "type": "object",
                            "additionalProperties": false,
                            "required": ["dayNumber", "title", "items"],
                            "properties": {
                              "dayNumber": {"type": "integer"},
                              "title": {"type": "string"},
                              "items": {
                                "type": "array",
                                "maxItems": 5,
                                "items": {
                                  "type": "object",
                                  "additionalProperties": false,
                                  "required": ["contentId", "order", "recommendedTime", "stayMinutes", "reason"],
                                  "properties": {
                                    "contentId": {"type": "string"},
                                    "order": {"type": "integer"},
                                    "recommendedTime": {
                                      "type": "string",
                                      "pattern": "^([01]\\\\d|2[0-3]):[0-5]\\\\d$"
                                    },
                                    "stayMinutes": {"type": "integer"},
                                    "reason": {"type": "string"}
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                    """);
        } catch (JsonProcessingException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
