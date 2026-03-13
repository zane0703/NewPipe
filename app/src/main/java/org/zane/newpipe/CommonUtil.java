package org.zane.newpipe;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
            double dec = num / 1_000_000_000.0;
            if (dec < 10) {
                return String.format("%.1fB", dec);
            } else {
                return String.format("%.0fB", dec);
            }
        }
        if (num >= 1_000_000) {
            double dec = num / 1_000_000.0;
            if (dec < 10) {
                return String.format("%.1fM", dec);
            } else {
                return String.format("%.0fM", dec);
            }
        }
        if (num >= 1_000) {
            double dec = num / 1_000.0;
            if (dec < 10) {
                return String.format("%.1fK", dec);
            } else {
                return String.format("%.0fK", dec);
            }
        }
        return Long.toString(num);
    }

    public static String formatRelativeTime(LocalDateTime pastTime) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime past = pastTime.atZone(ZoneId.systemDefault());
        Duration duration = Duration.between(past, now);

        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30; // Approximation for simplicity
        long years = days / 365; // Approximation for simplicity

        if (years > 0) {
            return years + (years == 1 ? " year ago" : " years ago");
        } else if (months > 0) {
            return months + (months == 1 ? " month ago" : " months ago");
        } else if (weeks > 0) {
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else if (days > 0) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (seconds > 0) {
            return seconds + (seconds == 1 ? " second ago" : " seconds ago");
        } else {
            return "just now";
        }
    }
}
