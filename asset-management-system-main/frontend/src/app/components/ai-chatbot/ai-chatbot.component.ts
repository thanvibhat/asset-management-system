import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LlmReportService } from '../../services/llm-report.service';

interface ChatMessage {
  type: 'user' | 'bot';
  text: string;
  suggestions?: string[];
  showReportAction?: boolean;
}

@Component({
  selector: 'app-ai-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chatbot.component.html',
  styleUrls: ['./ai-chatbot.component.css']
})
export class AiChatbotComponent implements AfterViewChecked {
  @ViewChild('chatBody') private chatBody!: ElementRef;

  isOpen = false;
  isLoading = false;
  userInput = '';
  messages: ChatMessage[] = [
    {
      type: 'bot',
      text: 'Hello! I am your AI Asset Analyst. I can help you analyze your fleet, find high-cost assets, or generate executive reports. What would you like to know?',
      suggestions: ['Top performers', 'Highest maintenance cost', 'Generate executive report']
    }
  ];

  constructor(private llmService: LlmReportService) {}

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    if (!this.userInput.trim() || this.isLoading) return;

    const question = this.userInput.trim();
    this.messages.push({ type: 'user', text: question });
    this.userInput = '';
    this.isLoading = true;

    this.llmService.askQuestion(question).subscribe({
      next: (res) => {
        this.messages.push({
          type: 'bot',
          text: res.answer,
          suggestions: res.suggestedQuestions,
          showReportAction: question.toLowerCase().includes('report') || question.toLowerCase().includes('summary')
        });
        this.isLoading = false;
      },
      error: (err) => {
        const errorMsg = err.error?.message || 'Sorry, I encountered an error while processing your request. Please check your API key or try again later.';
        this.messages.push({
          type: 'bot',
          text: errorMsg
        });
        this.isLoading = false;
      }
    });
  }

  useSuggestion(suggestion: string) {
    this.userInput = suggestion;
    this.sendMessage();
  }

  downloadReport() {
    this.llmService.downloadExecutiveReport();
  }

  private scrollToBottom(): void {
    try {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    } catch (err) {}
  }
}
