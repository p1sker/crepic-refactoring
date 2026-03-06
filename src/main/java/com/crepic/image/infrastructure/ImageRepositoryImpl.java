package com.crepic.image.infrastructure; // 패키지는 프로젝트 구조에 맞게 조정하세요.

import com.crepic.image.domain.Image;
import com.crepic.image.domain.ImageRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

// ⭐️ Q클래스 스태틱 임포트 (Querydsl 컴파일 후 생성되는 클래스들)
import static com.crepic.image.domain.QImage.image;
import static com.crepic.image.domain.QCategory.category;
import static com.crepic.member.domain.QMember.member;

@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Image> findImagesByCursor(Long lastImageId, int pageSize) {

        // 1. 쿼리 실행
        List<Image> fetch = queryFactory
                .selectFrom(image)
                .join(image.seller, member).fetchJoin()    // N+1 완벽 방어
                .join(image.category, category).fetchJoin() // N+1 완벽 방어
                .where(
                        ltImageId(lastImageId),  // ⭐️ 커서 조건 (동적 쿼리)
                        image.deletedAt.isNull() // Soft Delete 필터링
                )
                .orderBy(image.id.desc())
                .limit(pageSize + 1) // ⭐️ 핵심: 20개를 요청하면 21개를 가져와서 다음 페이지가 있는지 확인합니다.
                .fetch();

        // 2. 다음 페이지 유무 확인 (hasNext)
        boolean hasNext = false;
        if (fetch.size() > pageSize) {
            fetch.remove(pageSize); // 프론트엔드에게는 요청한 20개만 줘야 하므로 마지막 1개는 버림
            hasNext = true;
        }

        // 3. Slice 객체로 포장해서 반환
        return new SliceImpl<>(fetch, PageRequest.of(0, pageSize), hasNext);
    }

    /**
     * [동적 쿼리 조건]
     * 마지막 이미지 ID보다 '작은(lt)' 데이터만 가져옵니다. (최신순 내림차순이니까)
     * 만약 lastImageId가 null이면 첫 번째 페이지를 조회하는 것이므로 조건을 무시합니다.
     */
    private BooleanExpression ltImageId(Long lastImageId) {
        if (lastImageId == null) {
            return null;
        }
        return image.id.lt(lastImageId);
    }
}