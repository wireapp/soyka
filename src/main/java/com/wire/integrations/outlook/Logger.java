package com.wire.integrations.outlook;//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Logger {
    private final static java.util.logging.Logger LOGGER
            = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
    private static final AtomicInteger errorCount = new AtomicInteger();

    static {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.pdfbox.pdmodel.font").setLevel(Level.SEVERE);

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");

        ConsoleHandler stderr = new ConsoleHandler();
        stderr.setFormatter(new CustomFormatter());
        LOGGER.addHandler(stderr);
        LOGGER.setUseParentHandlers(false);
    }

    public static LogLine tag(Object tag) {
        return new LogLine(tag);
    }

    public static void info(String msg, Object... args) {
        new LogLine().info(msg, args);
    }

    public static void warning(String msg, Object... args) {
        new LogLine().warning(msg, args);
    }

    public static void error(String msg, Object... args) {
        new LogLine().error(msg, args);
    }

    public static int getErrorCount() {
        return errorCount.get();
    }

    public static void exception(Throwable throwable, String message, Object... args) {
        new LogLine().exception(throwable, message, args);
    }

    public static class LogLine {
        private final ArrayList<Object> tags = new ArrayList<>();

        private LogLine(Object tag) {
            tags.add(tag);
        }

        private LogLine() {
        }

        public void info(String msg, Object... args) {
            LOGGER.log(Level.INFO, String.format(msg, args), tags.toArray());
        }

        public void warning(String msg, Object... args) {
            LOGGER.log(Level.WARNING, String.format(msg, args), tags.toArray());
        }

        public void error(String msg, Object... args) {
            errorCount.incrementAndGet();
            LOGGER.log(Level.SEVERE, String.format(msg, args), tags.toArray());
        }

        public void exception(Throwable throwable, String message, Object... args) {
            errorCount.incrementAndGet();
            LOGGER.log(Level.SEVERE, String.format(message, args), throwable);
        }

        public LogLine tag(Object tag) {
            tags.add(tag);
            return this;
        }
    }

    static class CustomFormatter extends Formatter {
        private static void addTags(StringBuilder builder, LogRecord record) {
            Level l = record.getLevel();
            String lvl = l == Level.SEVERE
                    ? "level=E"
                    : String.format("level=%.1s", l);
            // Add log level info
            builder.append("|").append(lvl);
            if (record.getParameters() != null)
                for (Object value : record.getParameters()) {

                }
            builder.append("|");
        }

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            addTags(builder, record);
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }
    }
}
