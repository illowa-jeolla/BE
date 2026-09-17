package com.example.travel.domain.region.dto;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.enums.RegionType;

public record RegionResponse(Long regionId, String name, RegionType regionType,
                             Long parentId) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getId(), region.getName(), region.getRegionType(),
                region.getParent() == null ? null : region.getParent().getId());
    }
}
