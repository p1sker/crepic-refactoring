package com.crepic.image.application;

import com.crepic.image.domain.ImageRepository;
import com.crepic.image.dto.ImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final ImageRepository imageRepository;

    public Slice<ImageResponse> getAllImagesByCursor(Long lastImageId, int pageSize) {
        // Querydsl로 만든 No-Offset 쿼리 호출!
        return imageRepository.findImagesByCursor(lastImageId, pageSize)
                .map(ImageResponse::from);
    }
}