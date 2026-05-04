import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssetMetricsDto } from '../models/models';

export interface LlmReportRequest {
  question: string;
}

export interface LlmReportResponse {
  answer: string;
  metricsUsed: AssetMetricsDto[];
  suggestedQuestions: string[];
}

@Injectable({ providedIn: 'root' })
export class LlmReportService {
  private apiUrl = '/api/reports/llm/ask';

  constructor(private http: HttpClient) {}

  askQuestion(question: string): Observable<LlmReportResponse> {
    return this.http.post<LlmReportResponse>(this.apiUrl, { question });
  }

  downloadExecutiveReport(): void {
    this.http.get('/api/reports/llm/executive-pdf', { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'executive_asset_report.pdf';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
