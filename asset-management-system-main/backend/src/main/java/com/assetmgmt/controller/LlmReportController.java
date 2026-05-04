package com.assetmgmt.controller;

import com.assetmgmt.dto.AssetMetricsDto;
import com.assetmgmt.dto.LlmReportDto;
import com.assetmgmt.service.AnalyticsContextBuilder;
import com.assetmgmt.service.AssetAnalyticsService;
import com.assetmgmt.service.ExecutiveReportService;
import com.assetmgmt.service.GeminiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/reports/llm")
@RequiredArgsConstructor
public class LlmReportController {

    private final AssetAnalyticsService assetAnalyticsService;
    private final ExecutiveReportService executiveReportService;
    private final AnalyticsContextBuilder contextBuilder;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @GetMapping("/executive-pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<byte[]> downloadExecutiveReport() {
        byte[] pdf = executiveReportService.generateExecutivePdfReport();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "executive_asset_report.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    @PostMapping("/ask")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<LlmReportDto.Response> askAi(@RequestBody LlmReportDto.Request request) {
        List<AssetMetricsDto> allMetrics = assetAnalyticsService.getAllMetrics();
        String systemPrompt = contextBuilder.buildSystemPrompt(allMetrics);
        
        // 1. Primary AI response
        String answer = geminiService.ask(systemPrompt, request.getQuestion());

        // 2. Suggested questions
        String suggestionPrompt = "Based on this asset data, suggest 5 short follow-up questions a manager might ask. Return as a JSON array of strings only. Example: [\"Question 1\", \"Question 2\"]";
        String suggestionsRaw = geminiService.ask(systemPrompt, suggestionPrompt);
        
        List<String> suggestedQuestions;
        try {
            // Attempt to extract JSON array if AI adds markdown backticks
            String jsonOnly = suggestionsRaw.replaceAll("```json|```", "").trim();
            suggestedQuestions = objectMapper.readValue(jsonOnly, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            suggestedQuestions = new ArrayList<>();
            suggestedQuestions.add("What is the maintenance trend for high-cost assets?");
        }

        return ResponseEntity.ok(LlmReportDto.Response.builder()
                .answer(answer)
                .metricsUsed(allMetrics.stream().limit(10).toList()) // Include a sample of metrics used
                .suggestedQuestions(suggestedQuestions)
                .build());
    }
}
