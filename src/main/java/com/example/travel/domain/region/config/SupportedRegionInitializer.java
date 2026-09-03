package com.example.travel.domain.region.config;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SupportedRegionInitializer implements ApplicationRunner {
    private static final List<Seed> SUPPORTED_REGIONS = List.of(
            new Seed("전주", "35.8242000", "127.1480000"),
            new Seed("군산", "35.9677000", "126.7368000"),
            new Seed("남원", "35.4164000", "127.3904000"),
            new Seed("목포", "34.8118000", "126.3922000"),
            new Seed("광주", "35.1595000", "126.8526000"),
            new Seed("순천", "34.9506000", "127.4872000"),
            new Seed("여수", "34.7604000", "127.6622000"),
            new Seed("나주", "35.0160000", "126.7108000"),
            new Seed("광양", "34.9407000", "127.6959000"),
            new Seed("담양", "35.3213000", "126.9882000"),
            new Seed("곡성", "35.2820000", "127.2919000"),
            new Seed("구례", "35.2025000", "127.4628000"),
            new Seed("고흥", "34.6112000", "127.2850000"),
            new Seed("보성", "34.7715000", "127.0801000"),
            new Seed("화순", "35.0644000", "126.9866000"),
            new Seed("장흥", "34.6817000", "126.9069000"),
            new Seed("강진", "34.6421000", "126.7672000"),
            new Seed("해남", "34.5734000", "126.5989000"),
            new Seed("영암", "34.8002000", "126.6968000"),
            new Seed("무안", "34.9905000", "126.4817000"),
            new Seed("함평", "35.0659000", "126.5166000"),
            new Seed("영광", "35.2772000", "126.5119000"),
            new Seed("장성", "35.3019000", "126.7848000"),
            new Seed("완도", "34.3110000", "126.7551000"),
            new Seed("진도", "34.4868000", "126.2635000"),
            new Seed("신안", "34.8336000", "126.3513000")
    );

    private final RegionRepository regionRepository;

    public SupportedRegionInitializer(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SUPPORTED_REGIONS.stream()
                .filter(seed -> regionRepository.findActiveByName(seed.name()).isEmpty())
                .map(seed -> Region.createSupportedCity(seed.name(),
                        new BigDecimal(seed.latitude()), new BigDecimal(seed.longitude())))
                .forEach(regionRepository::save);
    }

    private record Seed(String name, String latitude, String longitude) {
    }
}
