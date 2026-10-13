package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateUtils() {
    }

    /** Timestamp safe for file names, e.g. 20261013_143005 */
    public static String fileTimestamp() {
        return LocalDateTime.now().format(FILE_STAMP);
    }
}
