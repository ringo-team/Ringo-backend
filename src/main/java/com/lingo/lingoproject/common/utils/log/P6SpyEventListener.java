package com.lingo.lingoproject.common.utils.log;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.P6SpyOptions;
import java.sql.SQLException;

/**
 * P6Spy JDBC 이벤트 리스너.
 *
 * <p>JDBC 연결이 생성될 때마다 {@link #onAfterGetConnection}이 호출됩니다.
 * 이 시점에 {@link P6SpyOptions}를 통해 커스텀 SQL 포맷터
 * ({@link P6spyPrettySqlFormatter})를 동적으로 설정합니다.</p>
 *
 * <h2>왜 런타임에 포맷터를 설정하나요?</h2>
 * <p>spy.properties의 {@code logMessageFormat}으로도 포맷터를 지정할 수 있지만,
 * Spring Bean으로 관리되는 포맷터를 사용하려면 이 방식이 필요합니다.
 * Spring 컨텍스트가 완전히 초기화된 후 JDBC 연결이 맺어지므로,
 * 이 시점에 포맷터를 교체해도 안전합니다.</p>
 *
 * <h2>P6SpyConfig와의 관계</h2>
 * <p>{@link P6SpyConfig}에서 Bean으로 등록되어 P6Spy 내부에서 자동으로 감지됩니다.</p>
 */
public class P6SpyEventListener extends JdbcEventListener {

  /**
   * JDBC Connection 획득 이후 호출됩니다.
   * P6SpyOptions에 커스텀 포맷터 클래스명을 등록하여 이후 모든 SQL 로그에 적용합니다.
   *
   * @param connectionInformation 연결 정보 (URL, 드라이버 등)
   * @param e                     연결 중 발생한 예외 (정상 연결 시 null)
   */
  @Override
  public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
    P6SpyOptions.getActiveInstance().setLogMessageFormat(P6spyPrettySqlFormatter.class.getName());
  }
}
