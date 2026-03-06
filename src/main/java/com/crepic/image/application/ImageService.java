package com.crepic.image.application;

import com.crepic.image.domain.Image;
import com.crepic.image.domain.ImageRepository;
import com.crepic.image.dto.ImageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final ImageRepository imageRepository;
    private final StringRedisTemplate redisTemplate; // ⭐️ 추가됨
    private final ObjectMapper objectMapper;         // ⭐️ JSON 파싱용 추가됨

    private static final String FIRST_PAGE_CACHE_KEY = "images:gallery:firstPage";
    private static final int CACHE_TTL_MINUTES = 5;

    /**
     * [메인 페이지 캐싱 + 무한 스크롤 조회]
     */
    public Slice<ImageResponse> getAllImagesByCursor(Long lastImageId, int pageSize) {

        // ⭐️ 1. S+++급 로직: 첫 페이지(lastImageId == null) 조회일 경우에만 Redis 캐시를 탄다.
        if (lastImageId == null && pageSize == 20) {
            try {
                // Redis에서 캐시 데이터 조회 (Look-aside Cache)
                String cachedData = redisTemplate.opsForValue().get(FIRST_PAGE_CACHE_KEY);

                if (cachedData != null) {
                    log.info("메인 페이지 이미지 Redis 캐시 Hit!");
                    // JSON String -> List<ImageResponse> 복원
                    List<ImageResponse> cachedList = objectMapper.readValue(cachedData, new TypeReference<List<ImageResponse>>() {});

                    // Slice 객체로 재조립하여 반환 (첫 페이지는 보통 다음 페이지가 있다고 가정(true)하거나, 사이즈로 계산)
                    boolean hasNext = cachedList.size() == pageSize;
                    return new SliceImpl<>(cachedList, PageRequest.of(0, pageSize), hasNext);
                }
            } catch (Exception e) {
                // 캐시 파싱 에러가 나더라도 서비스는 정상 동작해야 하므로 에러만 남기고 DB 조회로 넘어감
                log.error("Redis 캐시 파싱 에러 (무시하고 DB 조회 진행): {}", e.getMessage());
            }
        }

        // 2. 캐시 Miss (또는 2번째 페이지 이후 스크롤 조회) -> DB No-Offset 쿼리 직접 찌름
        Slice<Image> fetchResult = imageRepository.findImagesByCursor(lastImageId, pageSize);
        Slice<ImageResponse> responseSlice = fetchResult.map(ImageResponse::from);

        // ⭐️ 3. DB에서 첫 페이지를 가져온 경우라면, 다음 사람을 위해 Redis에 캐싱해둔다. (Write Cache)
        if (lastImageId == null && pageSize == 20 && responseSlice.hasContent()) {
            try {
                String jsonString = objectMapper.writeValueAsString(responseSlice.getContent());
                redisTemplate.opsForValue().set(FIRST_PAGE_CACHE_KEY, jsonString, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.info("메인 페이지 이미지 Redis 캐시 저장 완료");
            } catch (JsonProcessingException e) {
                log.error("Redis 캐시 저장 실패: {}", e.getMessage());
            }
        }

        return responseSlice;
    }
}