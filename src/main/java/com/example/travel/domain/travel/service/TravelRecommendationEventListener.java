package com.example.travel.domain.travel.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TravelRecommendationEventListener {
    private final TravelRecommendationProcessor processor;

    public TravelRecommendationEventListener(TravelRecommendationProcessor processor) {
        this.processor = processor;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TravelRecommendationCreatedEvent event) {
        processor.process(event.requestId());
    }
}
