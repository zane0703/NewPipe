package org.zane.newpipe.util;

import java.awt.Container;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class CommonUtil {

    private static DecimalFormat df = new DecimalFormat("#.#");

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
                return df.format(dec) + "B";
            } else {
                return String.format("%.0fB", dec);
            }
        }
        if (num >= 1_000_000) {
            double dec = num / 1_000_000.0;
            if (dec < 10) {
                return df.format(dec) + "M";
            } else {
                return String.format("%.0fM", dec);
            }
        }
        if (num >= 1_000) {
            double dec = num / 1_000.0;
            if (dec < 10) {
                return df.format(dec) + "M";
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

    public static JTextArea createUneditableTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        return textArea;
    }

    public static Map<String, String> getQueryMap(String query)
        throws ParseException {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String[] paramkv = param.split("=", 2);
            if (paramkv.length != 2) {
                throw new ParseException(
                    "Invalid format, expected key=value",
                    0
                );
            }
            map.put(paramkv[0], paramkv[1]);
        }
        return map;
    }

    public static boolean retryPrompt(Container container, String itemName) {
        Object[] options = { "Retry", "cancel" };
        return (
            JOptionPane.showOptionDialog(
                container,
                "Error loading " + itemName + " info do you wnat to retry",
                "Error",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]
            ) ==
            0
        );
    }
}
