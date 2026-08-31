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
            new Seed("보성", "34.7715000", "127.0801000"),
            new Seed("완도", "34.3110000", "126.7551000")
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
