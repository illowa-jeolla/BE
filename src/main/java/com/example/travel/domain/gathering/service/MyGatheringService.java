package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.dto.MyGatheringCandidate;
import com.example.travel.domain.gathering.dto.MyGatheringItem;
import com.example.travel.domain.gathering.dto.MyGatheringRequest;
import com.example.travel.domain.gathering.dto.MyGatheringResponse;
import com.example.travel.domain.gathering.enums.GatheringTiming;
import com.example.travel.domain.gathering.enums.MyGatheringType;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class MyGatheringService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final GatheringRepository gatheringRepository;
    private final Clock clock;

    public MyGatheringService(GatheringRepository gatheringRepository, Clock clock) {
        this.gatheringRepository = gatheringRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MyGatheringResponse findMine(Long userId, MyGatheringRequest request) {
        int pageNumber = request.page() == null ? DEFAULT_PAGE : request.page();
        int pageSize = request.size() == null ? DEFAULT_SIZE : request.size();
        PageRequest pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.ASC, "startsAt", "id"));

        MyGatheringType type = MyGatheringType.fromApiValue(request.type());
        Page<MyGatheringCandidate> page = type == MyGatheringType.HOSTED
                ? gatheringRepository.findHostedGatherings(
                        userId, ParticipantStatus.JOINED, pageable)
                : gatheringRepository.findJoinedGatherings(userId, ParticipantRole.MEMBER,
                        ParticipantStatus.JOINED, pageable);
        OffsetDateTime now = OffsetDateTime.now(clock);

        return new MyGatheringResponse(type.apiValue(), page.getContent().stream()
                .map(candidate -> toItem(candidate, now))
                .toList(), pageNumber, pageSize, page.getTotalElements(), page.hasNext());
    }

    private MyGatheringItem toItem(MyGatheringCandidate candidate, OffsetDateTime now) {
        GatheringTiming timing = candidate.startsAt().isAfter(now)
                ? GatheringTiming.UPCOMING : GatheringTiming.PAST;
        return new MyGatheringItem(candidate.id(), candidate.title(),
                new MyGatheringItem.RegionSummary(candidate.regionId(), candidate.regionName()),
                candidate.concept(), candidate.meetingPlace(), candidate.startsAt(),
                candidate.capacity(), candidate.participantCount(), candidate.status(), timing);
    }
}
