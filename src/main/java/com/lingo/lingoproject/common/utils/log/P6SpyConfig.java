package com.lingo.lingoproject.common.utils.log;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P6Spy SQL 로깅 설정 클래스.
 *
 * <h2>P6Spy란?</h2>
 * <p>P6Spy는 JDBC 드라이버를 프록시로 감싸서 실행되는 모든 SQL 쿼리를 가로채 로그로 출력하는 라이브러리입니다.
 * JPA/Hibernate가 생성하는 쿼리에 실제 바인딩된 파라미터 값을 포함하여 출력할 수 있습니다.</p>
 *
 * <h2>구성 요소</h2>
 * <ul>
 *   <li>{@link P6SpyEventListener}: JDBC 연결 시 커스텀 포맷터를 동적으로 등록</li>
 *   <li>{@link P6spyPrettySqlFormatter}: SQL을 보기 좋게 포맷팅하고 호출 스택 출력</li>
 *   <li>{@link P6SpyExcludeEmptyLog}: 빈 SQL 쿼리 로그 필터링</li>
 *   <li>spy.properties: P6Spy 전역 설정 파일 (appender, formatter, filter 등)</li>
 * </ul>
 *
 * <h2>spy.properties 설정</h2>
 * <pre>
 * appender=com.lingo.lingoproject.common.utils.log.P6SpyExcludeEmptyLog
 * logMessageFormat=com.lingo.lingoproject.common.utils.log.P6spyPrettySqlFormatter
 * filter=true
 * exclude=from appointments
 * </pre>
 *
 * <h2>운영 환경 주의사항</h2>
 * <p>P6Spy는 모든 SQL 쿼리를 로깅하므로 운영 환경에서는 성능 부하가 발생할 수 있습니다.
 * 운영 환경에서는 application-prod.yml에서 P6Spy 의존성을 제외하거나
 * spy.properties에서 {@code outagedetection=false}로 비활성화하는 것을 권장합니다.</p>
 */
@Configuration
public class P6SpyConfig {

    /**
     * P6Spy JDBC 이벤트 리스너를 Bean으로 등록합니다.
     * JDBC 연결 생성 시 {@link P6SpyEventListener#onAfterGetConnection}이 호출되어
     * 커스텀 포맷터({@link P6spyPrettySqlFormatter})를 동적으로 설정합니다.
     */
    @Bean
    public P6SpyEventListener p6SpyCustomEventListener() {
        return new P6SpyEventListener();
    }

    /**
     * 커스텀 SQL 포맷터를 Bean으로 등록합니다.
     * 실제 포맷팅은 {@link P6spyPrettySqlFormatter#formatMessage}에서 이루어집니다.
     */
    @Bean
    public P6spyPrettySqlFormatter p6SpyCustomFormatter() {
        return new P6spyPrettySqlFormatter();
    }
}