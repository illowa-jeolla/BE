package com.example.travel.domain.community.service;

import com.example.travel.domain.community.entity.TravelPostImage;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravelPostImageOrderService {
    private final TravelPostImageRepository imageRepository;

    public TravelPostImageOrderService(TravelPostImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public void reorder(Long postId, List<Long> imageIds) {
        if (imageIds == null) return;
        List<TravelPostImage> images = imageRepository
                .findAllByPostIdOrderByDisplayOrderAsc(postId);
        var requested = new HashSet<>(imageIds);
        var actual = images.stream().map(TravelPostImage::getId).collect(Collectors.toSet());
        if (imageIds.size() != images.size() || requested.size() != images.size()
                || !requested.equals(actual)) {
            throw new CommunityException(CommunityErrorCode.INVALID_IMAGE_ORDER);
        }
        var byId = images.stream().collect(Collectors.toMap(TravelPostImage::getId, image -> image));
        for (int index = 0; index < imageIds.size(); index++) {
            byId.get(imageIds.get(index)).changeDisplayOrder((short) (index + 10));
        }
        imageRepository.flush();
        for (int index = 0; index < imageIds.size(); index++) {
            byId.get(imageIds.get(index)).changeDisplayOrder((short) index);
        }
        imageRepository.flush();
    }
}
