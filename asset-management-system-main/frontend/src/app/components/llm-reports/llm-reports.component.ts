import { Component, ElementRef, ViewChild, AfterViewChecked, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LlmReportService, LlmReportResponse } from '../../services/llm-report.service';
import { AssetMetricsDto } from '../../models/models';
import { ActivatedRoute } from '@angular/router';

interface ChatMessage {
  role: 'user' | 'ai';
  text: string;
  timestamp: Date;
  metricsUsed?: AssetMetricsDto[];
  suggestedQuestions?: string[];
  showData?: boolean;
}

@Component({
  selector: 'app-llm-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './llm-reports.component.html',
  styleUrls: ['./llm-reports.component.css']
})
export class LlmReportsComponent implements AfterViewChecked, OnInit {
  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  userInput = '';
  loading = false;
  messages: ChatMessage[] = [];

  starterQuestions = [
    "Which asset costs the most to maintain?",
    "Show assets with poor value — low purchase cost but high maintenance",
    "Which assets have had the most repairs?",
    "What are our best performing assets?",
    "Which assets should we consider retiring?"
  ];

  constructor(
    private llmService: LlmReportService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['q']) {
        this.submitQuestion(params['q']);
      }
    });
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  submitQuestion(predefined?: string) {
    const question = predefined || this.userInput.trim();
    if (!question || this.loading) return;

    this.userInput = '';
    this.messages.push({
      role: 'user',
      text: question,
      timestamp: new Date()
    });

    this.loading = true;
    this.llmService.askQuestion(question).subscribe({
      next: (response: LlmReportResponse) => {
        this.messages.push({
          role: 'ai',
          text: response.answer,
          timestamp: new Date(),
          metricsUsed: response.metricsUsed,
          suggestedQuestions: response.suggestedQuestions,
          showData: false
        });
        this.loading = false;
      },
      error: (err) => {
        this.messages.push({
          role: 'ai',
          text: "I'm sorry, I encountered an error while analyzing the data. Please try again later.",
          timestamp: new Date()
        });
        this.loading = false;
      }
    });
  }

  downloadReport() {
    this.llmService.downloadExecutiveReport();
  }

  private scrollToBottom(): void {
    try {
      this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
    } catch (err) {}
  }
}
