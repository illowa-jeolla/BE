package com.example.travel.domain.ai.service;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AiMatchEventListener {
    private final AiMatchProcessor processor;
    public AiMatchEventListener(AiMatchProcessor processor) { this.processor = processor; }
    @Async @EventListener
    public void handle(AiMatchCreatedEvent event) { processor.process(event.requestId()); }
}
