package com.lingo.lingoproject.common.utils.log;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P6SpyExcludeEmptyLog extends Slf4JLogger {

  private Logger log;

  public P6SpyExcludeEmptyLog() {
    log = LoggerFactory.getLogger("p6spy");
  }

  @Override
  public void logSQL(int connectionId, String now, long elapsed,
      Category category, String prepared, String sql, String url){
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
