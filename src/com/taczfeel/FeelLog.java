package com.taczfeel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 统一日志前缀，方便在 latest.log 里搜 taczfeel。异常只打前几条，绝不刷屏。 */
public final class FeelLog {

    private static final Logger LOG = LogManager.getLogger("taczfeel");
    private static int softErrors = 0;

    private FeelLog() {
    }

    public static void info(String msg) {
        LOG.info("[taczfeel] " + msg);
    }

    public static void warn(String msg) {
        LOG.warn("[taczfeel] " + msg);
    }

    public static void debug(String msg) {
        if (FeelConfig.debugLog) {
            LOG.info("[taczfeel] " + msg);
        }
    }

    /**
     * 渲染 / 音效路径上的异常：吞掉但记录，避免因为一个参数写错就崩游戏。
     * 同一进程最多打 8 条，之后静默。
     */
    public static void softError(String where, Throwable t) {
        if (softErrors >= 8) {
            return;
        }
        softErrors++;
        LOG.warn("[taczfeel] {} failed ({}), skipped: {}", where, softErrors, String.valueOf(t));
        if (FeelConfig.debugLog) {
            LOG.warn("[taczfeel] stack trace", t);
        }
    }
}
