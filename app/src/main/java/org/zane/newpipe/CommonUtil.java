package org.zane.newpipe;

import java.time.Duration;

public class CommonUtil {

    public static String getTimeString(long time) {
        Duration duration = Duration.ofSeconds(time);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart(); // toMinutesPart() in Java 9+, use arithmetic for Java 8
        long seconds = duration.toSecondsPart(); // toSecondsPart() in Java 9+, use arithmetic for Java 8
        if (hours == 0) {
            return String.format("%02d:%02d", minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String numberToStringUnit(long num) {
        if (num >= 1_000_000_000) {
            return new StringBuilder(5)
                .append(num / 1_000_000_000)
                .append('B')
                .toString();
        }
        if (num >= 1_000_000) {
            return new StringBuilder(5)
                .append(num / 1_000_000)
                .append('M')
                .toString();
        }
        if (num >= 1_000) {
            return new StringBuilder(5)
                .append(num / 1_000)
                .append('K')
                .toString();
        }
        return Long.toString(num);
    }
}
