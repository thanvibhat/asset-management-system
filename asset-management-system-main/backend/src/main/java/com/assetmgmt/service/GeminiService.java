package com.assetmgmt.service;

import com.assetmgmt.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.name:gemini-1.5-flash}")
    private String modelName;

    private static final String PLACEHOLDER = "${GEMINI_API_KEY}";

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public String ask(String systemPrompt, String userQuestion) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals(PLACEHOLDER)) {
            throw new BusinessException("Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable.");
        }

        String url = BASE_URL + modelName + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> part = new HashMap<>();
        part.put("text", systemPrompt + "\n\n" + userQuestion);
        
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", Collections.singletonList(part));
        
        requestBody.put("contents", Collections.singletonList(content));
        
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestBody, Map.class);
            
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException("Gemini API returned status: " + responseEntity.getStatusCode());
            }

            Map<String, Object> response = responseEntity.getBody();
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> responseContent = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) responseContent.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            throw new BusinessException("Gemini API Error (" + e.getStatusCode() + "): " + errorBody);
        } catch (Exception e) {
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException("Failed to call Gemini API: " + e.getMessage());
        }
        throw new BusinessException("No valid response from Gemini API");
    }
}
