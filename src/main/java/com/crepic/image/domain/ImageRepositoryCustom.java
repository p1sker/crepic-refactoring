package com.crepic.image.domain;

import org.springframework.data.domain.Slice;

public interface ImageRepositoryCustom {
    // ⭐️ S+++급 핵심: Pageable 대신 '마지막으로 본 이미지 ID(Cursor)'와 '사이즈'를 받습니다.
    Slice<Image> findImagesByCursor(Long lastImageId, int pageSize);
}