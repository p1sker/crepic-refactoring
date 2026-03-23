# Crepic-Refactoring: High-Performance Coupon System

> 오차 없는 데이터 정합성과 4,393.7 TPS를 달성한 선착순 쿠폰 발급 시스템 리팩토링 프로젝트

본 프로젝트는 기존 레거시 시스템의 결제 및 이벤트 병목 현상을 진단하고, Phase 1부터 6까지 단계별 아키텍처 개선을 통해 대규모 트래픽 환경에서의 동시성 제어와 성능 최적화를 실천한 과정을 담고 있습니다.

---

## Tech Stack

- Language: Java 21
- Framework: Spring Boot 3.x, Spring Data JPA
- Database: PostgreSQL, Redis
- Messaging: Apache Kafka
- Test Tools: JMeter (Performance/Stress Test)

---

## Performance Evolution (Phase 1 ~ 6)

### Phase 1-3: RDBMS 기반의 정합성 확보와 병목 진단
- 목표: 1,000명 동시 요청 환경에서의 Lost Update 해결
- 핵심 기술: DB Pessimistic Lock (SELECT ... FOR UPDATE)
- 결과: 정합성 100% 확보했으나, DB 커넥션 풀 병목으로 인한 633 TPS 기록
- 시행착오: Redisson 분산 락 도입 시 RTT 증가로 인한 성능 하락(317 TPS) 경험 및 락 기반 직렬화의 한계 식별

### Phase 4-5: Redis 및 Kafka 기반 비동기 파이프라인 구축
- 목표: 10,000명 규모의 트래픽 스케일업 및 처리량 극대화
- 핵심 기술: Redis DECR(Lock-Free) + Kafka Messaging
- 최적화: Stack Trace 생성을 차단하는 요약 로깅 전략 및 Kafka Producer Batching 적용으로 I/O 블로킹 제거

### Phase 6: Lua Script 기반 어뷰징 방어 (Final Phase)
- 목표: 1인 1매 정책 위반 및 매크로 공격 방어
- 핵심 기술: Redis Set + Lua Script (Atomic Transaction)
- 로직 검증: 단일 유저 100회 무차별 연사 테스트(Thread 1 / Loop 100) 시 최초 1회 승인 및 99회 즉시 차단(Fail-Fast) 확인
- 성능 유지: 중복 검증 로직 추가 후에도 10,000명 동시 접속 환경에서 성능 저하 없이 안정적 처리 확인

---

## Final Benchmark Result

- 측정 도구: JMeter
- 테스트 조건 (성능): 10,000 Threads / 1s Ramp-up / 1 Loop
- 테스트 조건 (정합성): 1 Thread / 100 Loops (memberId 고정 어뷰징 테스트)
- 최종 처리량: 4,393.7 TPS
- 데이터 정합성: 10,000건 이상의 동시 요청 중 정확히 재고만큼(100건) 발급 완료 (Error 0%)

---

## Key Takeaways

1. 정합성 vs 성능의 트레이드오프
비관적 락에서 분산 락, 그리고 메시지 큐 기반의 비동기 아키텍처로 진화하며 각 단계에서의 지연 시간(Latency)과 처리량(Throughput)의 상관관계를 정량적으로 이해했습니다.

2. Fail-Fast 아키텍처의 중요성
비즈니스 룰 검증을 무거운 RDBMS 계층이 아닌 최상단 메모리 계층(Redis)에서 처리함으로써 시스템 전체의 가용성을 보호하고 불필요한 리소스 낭비를 차단하는 설계의 위력을 체감했습니다.

3. 로깅 전략과 시스템 성능
대규모 트래픽 환경에서는 코드 한 줄의 상세 로깅(Stack Trace 등)이 디스크 I/O 병목을 유발하여 전체 시스템 처리량을 수배 이상 좌우할 수 있음을 실험을 통해 증명했습니다.
