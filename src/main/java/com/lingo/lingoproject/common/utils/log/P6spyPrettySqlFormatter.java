package com.lingo.lingoproject.common.utils.log;

import static java.util.Arrays.stream;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Stack;
import java.util.function.Predicate;
import org.hibernate.engine.jdbc.internal.FormatStyle;

/**
 * P6Spy SQL 포맷터 — SQL을 보기 좋게 포맷팅하고 호출 스택을 함께 출력합니다.
 *
 * <h2>출력 형식</h2>
 * <pre>
 * [포맷팅된 SQL 쿼리]
 *
 *   Connection ID: 1
 *   Execution Time: 5 ms
 *
 *   Call Stack (number 1 is entry point):
 *     1. io.p6spy.engine...
 *     2. io.p6spy.engine...
 *     ...
 * ----------------------------------------------------------------------------------------------------
 * </pre>
 *
 * <h2>SQL 포맷팅 규칙</h2>
 * <ul>
 *   <li>DDL(CREATE, ALTER, COMMENT): Hibernate {@link FormatStyle#DDL} 포맷터 적용</li>
 *   <li>DML(SELECT, INSERT, UPDATE, DELETE): Hibernate {@link FormatStyle#BASIC} 포맷터 적용</li>
 *   <li>빈 SQL: null 반환 (로그 미출력)</li>
 * </ul>
 *
 * <h2>호출 스택 필터링</h2>
 * <p>{@code isExcludeWords()}는 P6Spy 내부 클래스({@code io.p6spy.*})의 스택 프레임만 포함하고
 * 이 포맷터 클래스 자신({@code P6spyPrettySqlFormatter})은 제외합니다.
 * 스택은 LIFO로 쌓이므로 {@link Stack}을 이용해 entry point(1번)가 맨 위에 오도록 역순으로 출력합니다.</p>
 *
 * <h2>+0900 제거</h2>
 * <p>Hibernate 포맷터가 타임존 오프셋(+0900)을 SQL에 삽입하는 경우가 있어
 * 가독성을 위해 제거합니다.</p>
 */
public class P6spyPrettySqlFormatter implements MessageFormattingStrategy {

  private static final String NEW_LINE = System.lineSeparator();
  private static final String P6SPY_FORMATTER = "P6spyPrettySqlFormatter";
  /** P6Spy 내부 패키지 — 호출 스택 필터링에 사용 */
  private static final String PACKAGE = "io.p6spy";
  private static final String CREATE = "create";
  private static final String ALTER = "alter";
  private static final String COMMENT = "comment";

  /**
   * SQL과 메타 정보를 포맷팅하여 로그 문자열을 반환합니다.
   * P6Spy가 각 SQL 실행 후 이 메서드를 호출합니다.
   *
   * @param connectionId JDBC 연결 ID
   * @param now          실행 시각
   * @param elapsed      실행 시간 (ms)
   * @param category     쿼리 카테고리 (STATEMENT, COMMIT 등)
   * @param prepared     ? 파라미터 원본 쿼리
   * @param sql          실제 값이 바인딩된 최종 쿼리
   * @param url          JDBC 연결 URL
   * @return 포맷팅된 로그 문자열 (빈 SQL이면 null)
   */
  @Override
  public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared,
      String sql, String url) {
    return sqlFormat(sql, category, getMessage(connectionId, elapsed, getStackBuilder()));
  }

  /**
   * SQL 본문을 포맷팅하고 메타 정보를 조합합니다.
   *
   * @param sql     실행된 SQL
   * @param category 쿼리 카테고리
   * @param message  Connection ID, 실행 시간, 호출 스택 정보
   * @return 조합된 최종 로그 문자열 (빈 SQL이면 null)
   */
  private String sqlFormat(String sql, String category, String message) {
    if (sql.trim().isEmpty()) {
      return null;
    }
    return NEW_LINE
        + sqlFormat(sql, category)
        + message;
  }

  /**
   * SQL 종류(DDL/DML)에 따라 Hibernate 포맷터를 적용합니다.
   */
  private String sqlFormat(String sql, String category) {
    if (isStatementDDL(sql, category)) {
      return FormatStyle.DDL
          .getFormatter()
          .format(sql)
          .replace("+0900", "");
    }
    return FormatStyle.BASIC
        .getFormatter()
        .format(sql)
        .replace("+0900", "");
  }

  /** DDL 쿼리(STATEMENT 카테고리이고 CREATE/ALTER/COMMENT로 시작)인지 확인합니다. */
  private boolean isStatementDDL(String sql, String category) {
    return isStatement(category) && isDDL(sql.trim().toLowerCase(Locale.ROOT));
  }

  /** P6Spy의 STATEMENT 카테고리인지 확인합니다. */
  private boolean isStatement(String category) {
    return Category.STATEMENT.getName().equals(category);
  }

  /** SQL이 DDL 구문(create/alter/comment)으로 시작하는지 확인합니다. */
  private boolean isDDL(String lowerSql) {
    return lowerSql.startsWith(CREATE) || lowerSql.startsWith(ALTER) || lowerSql.startsWith(COMMENT);
  }

  /**
   * Connection ID, 실행 시간, 호출 스택을 포함한 메타 정보 문자열을 생성합니다.
   */
  private String getMessage(int connectionId, long elapsed, StringBuilder callStackBuilder) {
    return NEW_LINE
        + NEW_LINE
        + "\t" + String.format("Connection ID: %s", connectionId)
        + NEW_LINE
        + "\t" + String.format("Execution Time: %s ms", elapsed)
        + NEW_LINE
        + NEW_LINE
        + "\t" + String.format("Call Stack (number 1 is entry point): %s", callStackBuilder)
        + NEW_LINE
        + NEW_LINE
        + "----------------------------------------------------------------------------------------------------";
  }

  /**
   * 현재 스레드의 호출 스택을 분석하여 P6Spy 내부 호출 경로만 추출합니다.
   *
   * <p>Stack(LIFO) 자료구조를 사용하여 호출 순서를 역전시킵니다.
   * Stream으로 스택 트레이스를 순차적으로 push하면 가장 마지막 호출이 top에 쌓이고,
   * pop으로 꺼낼 때 호출 순서의 역순(1번이 entry point)으로 출력됩니다.</p>
   */
  private StringBuilder getStackBuilder() {
    Stack<String> callStack = new Stack<>();
    stream(new Throwable().getStackTrace())
        .map(StackTraceElement::toString)
        .filter(isExcludeWords())
        .forEach(callStack::push);

    int order = 1;
    StringBuilder callStackBuilder = new StringBuilder();
    while (!callStack.empty()) {
      callStackBuilder.append(MessageFormat.format("{0}\t\t{1}. {2}", NEW_LINE, order++, callStack.pop()));
    }
    return callStackBuilder;
  }

  /**
   * P6Spy 내부 패키지({@code io.p6spy.*})의 스택 프레임만 포함하고,
   * 이 포맷터 클래스 자신은 제외하는 필터를 반환합니다.
   */
  private Predicate<? super String> isExcludeWords() {
    return charSequence -> charSequence.startsWith(PACKAGE) && !charSequence.contains(P6SPY_FORMATTER);
  }
}