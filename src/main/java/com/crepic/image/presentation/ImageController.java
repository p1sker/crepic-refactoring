package com.crepic.image.presentation;

import com.crepic.image.application.ImageService;
import com.crepic.image.dto.ImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Image", description = "이미지 갤러리 API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "이미지 무한 스크롤 조회 (No-Offset)", description = "커서(lastImageId) 기반으로 최신 이미지를 초고속으로 조회합니다.")
    @GetMapping(produces = "application/json")
    public ResponseEntity<Slice<ImageResponse>> getImages(
            // ⭐️ 첫 페이지 조회 시에는 lastImageId를 안 보내거나 null로 보냅니다.
            @RequestParam(required = false) Long lastImageId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(imageService.getAllImagesByCursor(lastImageId, size));
    }
}