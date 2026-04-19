package com.lingo.lingoproject.matching.domain.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * 추천 이성 알고리즘의 도메인 로직을 담당하는 Domain Service.
 *
 * <p>추천 후보 랜덤 선정, 설문 응답 점수 계산 등
 * 순수 비즈니스 규칙을 캡슐화한다. 인프라(DB/Redis) 의존이 없다.</p>
 */
@Service
public class RecommendationDomainService {

  /**
   * Fisher-Yates 셔플로 후보 목록에서 최대 {@code maxSize}명을 랜덤 선정한다.
   *
   * <p>원본 리스트를 직접 수정하므로 호출 전에 방어적 복사가 필요하면
   * 호출자가 직접 처리해야 한다.</p>
   *
   * @param candidates 전체 후보 ID 목록 (변경됨)
   * @param maxSize    최대 선정 인원
   * @return 랜덤 선정된 후보 ID 목록 (candidates의 앞부분 서브리스트)
   */
  public List<Long> selectRandomCandidates(List<Long> candidates, int maxSize) {
    int n = Math.min(candidates.size(), maxSize);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < n; i++) {
      int j = random.nextInt(i, candidates.size());
      Collections.swap(candidates, i, j);
    }
    return candidates.subList(0, n);
  }

  /**
   * 두 유저의 설문 응답 쌍에 대한 점수를 계산한다.
   *
   * <p>응답 차이가 적을수록, 극단값(1 또는 5)일수록 높은 점수를 부여한다.</p>
   * <pre>
   *   (5,5) → 5, (5,4) → 3, (5,3) → 1
   *   (4,4) → 4, (4,3) → 2, (4,2) → 0
   *   (3,3) → 4, (3,2) → 2  (3,1) → 1
   *   (2,2) → 4  (2,1) → 3
   *   (1,1) → 5
   * </pre>
   *
   * @param answer         한 유저의 응답 (1~5)
   * @param confrontAnswer 상대 유저의 응답 (1~5)
   * @return 계산된 매칭 점수
   */
  public int calculateAnswerPairScore(int answer, int confrontAnswer) {
    int score = 0;

    if (Math.abs(answer - confrontAnswer) == 0)      score += 4;
    else if (Math.abs(answer - confrontAnswer) == 1) score += 2;

    if ((answer == 5 || answer == 1) || (confrontAnswer == 5 || confrontAnswer == 1)) score += 1;

    return score;
  }

  /**
   * 개인 설문 응답의 극단성 점수를 계산한다.
   *
   * <p>추천 장소 우선순위 결정에 사용한다.
   * 1 또는 5처럼 극단적인 값일수록 더 높은 점수를 부여한다.</p>
   *
   * @param answer 응답값 (1~5)
   * @return 1(극단값), 0(준극단값), -1(중간값)
   */
  public int calculateAnswerScore(int answer) {
    if (answer == 1 || answer == 5)      return 1;
    else if (answer == 2 || answer == 4) return 0;
    else                                  return -1;
  }
}