package com.assetmgmt.dto;

import lombok.*;
import java.util.List;

public class LlmReportDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private String question;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private String answer;
        private List<AssetMetricsDto> metricsUsed;
        private List<String> suggestedQuestions;
    }
}
