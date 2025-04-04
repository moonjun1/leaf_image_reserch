package com.pam.pam.service;


import com.pam.pam.config.GeminiConfig;
import com.pam.pam.dto.CropResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class CropIdentifierService {

    private final RestTemplate restTemplate;
    private final GeminiConfig geminiConfig;

    public CropResponseDto identifyCrop(MultipartFile image) throws Exception {
        // 이미지를 Base64로 인코딩
        byte[] imageBytes = image.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Gemini API 요청 본문 구성
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> contents = new HashMap<>();
        contents.put("role", "user");

        // 텍스트 부분
// 텍스트 부분 - 기존 프롬프트 수정
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "이 농작물 사진을 분석하고 다음 형식으로 정확히 답변해주세요.\n"
                + "작물 이름: [작물 이름을 여기에 작성]\n"
                + "작물 설명: [작물에 대한 상세 설명을 여기에 작성]\n"
                + "재배 방법: [구체적인 재배 방법을 여기에 작성]\n\n"
                + "각 항목을 위 형식의 레이블과 함께 작성해주세요. 작물을 식별할 수 없는 경우 '알 수 없음'이라고 응답해주세요.");
        // 이미지 부분
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("mime_type", image.getContentType());
        imageData.put("data", base64Image);
        imagePart.put("inline_data", imageData);

        // parts 배열에 추가
        contents.put("parts", new Object[]{textPart, imagePart});

        requestBody.put("contents", new Object[]{contents});
        requestBody.put("generationConfig", Map.of(
                "temperature", 0.4,
                "topK", 32,
                "topP", 1.0,
                "maxOutputTokens", 2048
        ));

        // HTTP 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Gemini API 호출
        String url = geminiConfig.getApiUrl();
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        // API 응답 파싱
        String cropName = "알 수 없음";
        String description = "설명을 가져올 수 없습니다.";
        String cultivationMethod = "재배 방법을 가져올 수 없습니다.";

        if (response != null && response.containsKey("candidates")) {
            List<?> candidates = (List<?>) response.get("candidates");

            if (!candidates.isEmpty()) {
                Map<String, Object> candidate = (Map<String, Object>) candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                List<?> parts = (List<?>) content.get("parts");

                if (!parts.isEmpty()) {
                    Map<String, Object> part = (Map<String, Object>) parts.get(0);
                    String text = (String) part.get("text");

                    // 전체 응답 로깅 (디버깅용)
                    System.out.println("API 응답 전문: " + text);

                    // 정규식을 사용한 보다 강력한 파싱
                    Pattern namePattern = Pattern.compile("작물\\s*이름\\s*:\\s*(.+?)(?=\\n|$)");
                    Pattern descPattern = Pattern.compile("작물\\s*설명\\s*:\\s*(.+?)(?=\\n|$)");
                    Pattern cultPattern = Pattern.compile("재배\\s*방법\\s*:\\s*(.+?)(?=\\n|$)");

                    Matcher nameMatcher = namePattern.matcher(text);
                    if (nameMatcher.find()) {
                        cropName = nameMatcher.group(1).trim();
                    }

                    Matcher descMatcher = descPattern.matcher(text);
                    if (descMatcher.find()) {
                        description = descMatcher.group(1).trim();
                    }

                    Matcher cultMatcher = cultPattern.matcher(text);
                    if (cultMatcher.find()) {
                        cultivationMethod = cultMatcher.group(1).trim();
                    } else {
                        // 여러 줄로 된 재배 방법 처리
                        Pattern multLineCultPattern = Pattern.compile("재배\\s*방법\\s*:([\\s\\S]+?)(?=\\n\\n|$)");
                        Matcher multiLineMatcher = multLineCultPattern.matcher(text);
                        if (multiLineMatcher.find()) {
                            cultivationMethod = multiLineMatcher.group(1).trim();
                        }
                    }
                }
            }
        }
        // DTO로 변환하여 반환
        return CropResponseDto.builder()
                .cropName(cropName)
                .description(description)
                .cultivationMethod(cultivationMethod)
                .build();
    }
}