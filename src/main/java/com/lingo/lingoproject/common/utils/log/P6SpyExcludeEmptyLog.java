package com.lingo.lingoproject.common.utils.log;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 빈 SQL 쿼리 로그를 제외하는 P6Spy 커스텀 Appender.
 *
 * <h2>역할</h2>
 * <p>P6Spy는 JDBC 호출 중 내부적으로 빈 문자열 SQL이 실행되는 경우가 있습니다.
 * 이 클래스는 {@link Slf4JLogger}를 확장하여 {@code sql.trim().isEmpty()}인 경우
 * 로그를 출력하지 않도록 필터링합니다.</p>
 *
 * <h2>spy.properties 설정</h2>
 * <pre>
 * appender=com.lingo.lingoproject.common.utils.log.P6SpyExcludeEmptyLog
 * </pre>
 *
 * <h2>로그 레벨 매핑</h2>
 * <p>P6Spy Category를 SLF4J 로그 레벨로 변환합니다:
 * <ul>
 *   <li>{@link Category#ERROR} → log.error()</li>
 *   <li>{@link Category#WARN} → log.warn()</li>
 *   <li>{@link Category#DEBUG} → log.debug()</li>
 *   <li>그 외 (STATEMENT 등) → log.info()</li>
 * </ul>
 * </p>
 *
 * <h2>로그 출력 이름</h2>
 * <p>Logger 이름을 "p6spy"로 고정하여, logback.xml에서
 * {@code <logger name="p6spy">} 설정으로 SQL 로그만 별도 관리할 수 있습니다.</p>
 */
public class P6SpyExcludeEmptyLog extends Slf4JLogger {

  /** "p6spy" 이름의 SLF4J 로거. logback.xml에서 별도 설정 가능합니다. */
  private Logger log;

  public P6SpyExcludeEmptyLog() {
    log = LoggerFactory.getLogger("p6spy");
  }

  /**
   * SQL 로그를 출력합니다. 빈 SQL은 출력하지 않습니다.
   *
   * <p>{@code strategy.formatMessage()}는 spy.properties의
   * {@code logMessageFormat}에 설정된 포맷터({@link P6spyPrettySqlFormatter})를 호출합니다.</p>
   *
   * @param connectionId JDBC 연결 ID
   * @param now          실행 시각 (epoch ms 문자열)
   * @param elapsed      쿼리 실행 시간 (ms)
   * @param category     로그 카테고리 (STATEMENT, ERROR 등)
   * @param prepared     ? 자리표시자가 있는 원본 쿼리
   * @param sql          실제 파라미터가 바인딩된 쿼리
   * @param url          JDBC 연결 URL
   */
  @Override
  public void logSQL(int connectionId, String now, long elapsed,
      Category category, String prepared, String sql, String url){
    // 빈 SQL 쿼리는 로그 출력 생략 (P6Spy 내부 동작 중 발생하는 빈 실행 제외)
    if (sql.trim().isEmpty()) return;

    final String msg = strategy.formatMessage(connectionId, now, elapsed,
        category.toString(), prepared, sql, url);

    if (Category.ERROR.equals(category)) {
      log.error(msg);
    } else if (Category.WARN.equals(category)) {
      log.warn(msg);
    } else if (Category.DEBUG.equals(category)) {
      log.debug(msg);
    } else {
      log.info(msg);
    }
  }
}
