package romever.scan.oasisscan.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 本类所有时间，都强制返回 UTC 时间
 */
public abstract class Times {
    public static final String ZONE_BJ_ID = "+8";
    public static final ZoneOffset BEIJING_ZONE = ZoneOffset.of(ZONE_BJ_ID);
    public static final String ZONE_UTC_ID = "+0";
    public static final ZoneOffset UTC_ZONE = ZoneOffset.of(ZONE_UTC_ID);
    public static final ZoneId BEIJING_ZONE_ID = ZoneId.of(ZONE_BJ_ID);
    public static final ZoneId UTC_ZONE_ID = ZoneId.of(ZONE_UTC_ID);
    // 2019-09-24T14:13:19.940Z
//    public static final DateTimeFormatter DATETIME_SIMPLEX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATETIME_SHORT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /************************************* 获得当前UTC时间 **************************************************************/
    /**
     * 获得 UTC 的当前时间
     *
     * @return
     */

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static long nowEpochMilli() {
        return nowUtc().toEpochMilli();
    }

    public static long nowEpochSecond() {
        return nowUtc().getEpochSecond();
    }

    /**
     * 获得 UTC 的当前时间
     *
     * @return
     */
    public static LocalDateTime nowUtcDateTime() {
        return LocalDateTime.now(UTC_ZONE_ID);
    }

    public static LocalDateTime nowBjDateTime() {
        LocalDateTime localDateTime = nowUtcDateTime();
        return utcToBj(localDateTime);
    }
   public static LocalDateTime nowBjZeroDateTime() {
       final LocalDateTime localDateTime = nowBjDateTime();
       final String s = Times.formatDate(localDateTime);
       final LocalDateTime parse = parse(s + " 00:00:00");
       return parse;
   }

    public static long toEpochMilli(LocalDateTime time) {
        return time.toInstant(UTC_ZONE).toEpochMilli();
    }

    public static long toEpochMilli(Instant instant) {
        return instant.toEpochMilli();
    }

    public static long toEpochSecond(LocalDateTime time) {
        return time.toInstant(UTC_ZONE).getEpochSecond();
    }

    public static long toEpochSecond(Instant instant) {
        return instant.getEpochSecond();
    }

    /************************************************************************************************************/

    /************************************* **************************************************************/

    /**
     * 返回当前时间（UTC) 的 yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String formatUtc() {
        return formatUtc(DATETIME_FORMATTER);
    }

//    public static String formatUtc2SimplexFormat() {
//        return formatUtc(DATETIME_SIMPLEX_FORMATTER);
//    }
//    public static String formatUtc2SimplexFormat(LocalDateTime time) {
//        return format(time, DATETIME_SIMPLEX_FORMATTER);
//    }

    public static String formatUtcTime() {
        return format(nowUtcDateTime(), TIME_FORMATTER);
    }

    public static String formatUtc(DateTimeFormatter pattern) {
        return format(nowUtcDateTime(), pattern);
    }

    public static String formatUtc(String pattern) {

        return format(nowUtcDateTime(), DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 根据参数格式化
     * 
     * @param time
     * @return
     */
    public static String format(LocalDateTime time) {
        return format(time, DATETIME_FORMATTER);
    }

    public static String format(LocalDateTime time, String pattern) {
        return format(time, DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatTime(LocalDateTime time) {
        return format(time, TIME_FORMATTER);
    }
    public static String formatDate(LocalDateTime time) {
        return format(time, DATE_FORMATTER);
    }

    public static String format(LocalDateTime time, DateTimeFormatter pattern) {
        return time.format(pattern);
    }

    /***************************** 时区转换 *************************/

    /**
     * 需要保证传入的 LocalDateTime 表示的是 UTC 时间
     * 
     * @param localDateTime
     * @return
     */
    public static LocalDateTime utcToBj(LocalDateTime localDateTime) {
        return utcTo(localDateTime, BEIJING_ZONE_ID);
    }

    public static LocalDateTime bjToUtc(LocalDateTime localDateTime) {
        return toUtc(localDateTime, BEIJING_ZONE);
    }

    public static LocalDateTime utcTo(LocalDateTime localDateTime, ZoneId zoneId) {
        LocalDateTime localDateTime1 = LocalDateTime.ofInstant(localDateTime.toInstant(UTC_ZONE), zoneId);
        return localDateTime1;
    }

    public static LocalDateTime toUtc(LocalDateTime localDateTime, ZoneOffset zoneId) {
        LocalDateTime localDateTime1 = LocalDateTime.ofInstant(localDateTime.toInstant(zoneId), UTC_ZONE);
        return localDateTime1;
    }

    /***************************************************************************************************/

    public static LocalDateTime parse(String text, String pattern) {
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }
    public static LocalDateTime parse(String text, DateTimeFormatter pattern) {
        return LocalDateTime.parse(text, pattern);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, DATETIME_FORMATTER);
    }

    public static LocalDateTime parseDay(String text) {
        return LocalDate.parse(text, DATE_FORMATTER).atStartOfDay();
    }

    public static Date toDate(LocalDateTime text) {
        return Date.from(text.toInstant(UTC_ZONE));
    }

    public static LocalDateTime toLocalDateTime(Date text) {
        return toLocalDateTime(Instant.ofEpochMilli(text.getTime()));
    }

    public static LocalDateTime toLocalDateTime(Instant text) {
        return text.atZone(UTC_ZONE).toLocalDateTime();
    }

    public static LocalDateTime toLocalDateTime(long epochMilli) {
        return toLocalDateTime(Instant.ofEpochMilli(epochMilli));
    }

    public static LocalDateTime parseTime(String text) {
        return LocalDateTime.parse(text, TIME_FORMATTER);
    }

    public static LocalDateTime ofEpochMilli(long epochMilli) {
        return toLocalDateTime(epochMilli);
    }

    public static LocalDateTime plusSeconds(int seconds) {
        return plusSeconds(Instant.now(), seconds);
    }

    public static LocalDateTime plusMinutes(int seconds) {
        return plusMinutes(nowUtcDateTime(), seconds);
    }

    public static LocalDateTime plusSeconds(LocalDateTime time, int seconds) {
        return plusSeconds(time.toInstant(UTC_ZONE), seconds);
    }

    public static LocalDateTime plusSeconds(Instant instant, int seconds) {
        return instant.plusSeconds(seconds).atZone(UTC_ZONE).toLocalDateTime();
    }
//
//    public static LocalDateTime plusMinutes(Instant instant, int seconds) {
//        return instant.plusMinutes(seconds ).atZone(UTC_ZONE).toLocalDateTime();
//    }
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, int seconds) {
        return dateTime.toInstant(UTC_ZONE).atZone(UTC_ZONE).plusMinutes(seconds).toLocalDateTime();
    }

    public static LocalDateTime plusDays(int days) {
        return plusDays(Instant.now(), days);
    }

    public static LocalDateTime plusDays(LocalDateTime dateTime, int days) {
        return dateTime.toInstant(UTC_ZONE).atZone(UTC_ZONE).plusDays(days).toLocalDateTime();
    }

    public static LocalDateTime plusDays(Instant instant, int days) {
        return instant.atZone(UTC_ZONE).plusDays(days).toLocalDateTime();
    }

    public static LocalDateTime plusMonths(int n) {
        return plusMonths(Instant.now(), n);
    }

    public static LocalDateTime plusMonths(Instant instant, int n) {
        return instant.atZone(UTC_ZONE).plusMonths(n).toLocalDateTime();
    }

    public static LocalDateTime plusMonths(LocalDateTime time, int n) {
        return plusMonths(time.toInstant(UTC_ZONE), n);
    }

    public static LocalDateTime latestOClock(int hour) {
        LocalDateTime now = Times.nowUtcDateTime();
        if (now.getHour() >= hour) {
            now = now.plusDays(1);
        }
        return now.withHour(hour).truncatedTo(ChronoUnit.HOURS);
    }

    public static boolean isHourLater(int clock) {
        LocalDateTime now = Times.nowUtcDateTime();
        if (now.getHour() >= clock) {
            return true;
        }
        return false;
    }

    public static LocalDateTime truncatedToDay(int days) {

        return LocalDate.now().plusDays(days).atStartOfDay();
    }

    public static LocalDateTime firstDayOfMonth(int months) {
        LocalDate date = LocalDate.now().plusMonths(months);
        return date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    public static LocalDateTime lastDayOfMonth(int months) {
        LocalDate date = LocalDate.now().plusMonths(months);
        return date.with(TemporalAdjusters.lastDayOfMonth()).atStartOfDay();
    }

    public static boolean isLastDayOfMonth() {
        LocalDate date = LocalDate.now();
        LocalDate lastDate = date.with(TemporalAdjusters.lastDayOfMonth());
        return date == lastDate;
    }

    public static int monthsDuration(LocalDateTime finish) {
        return Period
                .between(LocalDate.now(), LocalDateTime.ofInstant(finish.toInstant(UTC_ZONE), UTC_ZONE).toLocalDate())
                .getMonths();
    }

    public static int millisDuration(LocalDateTime time) {

        return millisDuration(time, nowUtcDateTime());
    }

    public static int millisDuration(LocalDateTime start, LocalDateTime finish) {
        return (int) ChronoUnit.MILLIS.between(start.toInstant(UTC_ZONE), finish.toInstant(UTC_ZONE));
    }

    public static int secondsDuration(LocalDateTime start, LocalDateTime finish) {
        return (int) ChronoUnit.SECONDS.between(start.toInstant(UTC_ZONE), finish.toInstant(UTC_ZONE));
    }

    public static int minutesDuration(LocalDateTime start, LocalDateTime finish) {
        return (int) ChronoUnit.MINUTES.between(start.toInstant(UTC_ZONE), finish.toInstant(UTC_ZONE));
    }

    public static int hourDuration(LocalDateTime start, LocalDateTime finish) {
        return (int) ChronoUnit.HOURS.between(start.toInstant(UTC_ZONE), finish.toInstant(UTC_ZONE));
    }

    public static long daysDuration(LocalDateTime time) {
        LocalDate localDate = LocalDateTime.ofInstant(time.toInstant(UTC_ZONE), UTC_ZONE).toLocalDate();

        return LocalDate.now().toEpochDay() - localDate.toEpochDay();
    }

    public static long daysDuration(LocalDateTime start, LocalDateTime finish) {
        LocalDate localDate = LocalDateTime.ofInstant(start.toInstant(UTC_ZONE), UTC_ZONE).toLocalDate();
        LocalDate finishDate = LocalDateTime.ofInstant(finish.toInstant(UTC_ZONE), UTC_ZONE).toLocalDate();
        return finishDate.toEpochDay() - localDate.toEpochDay();
    }

    public static boolean isNowBefore(LocalDateTime time) {
        return Instant.now().isBefore(time.toInstant(UTC_ZONE));
    }

    public static boolean isNowBefore(LocalDateTime time, int seconds) {
        return nowUtc().isBefore(time.toInstant(UTC_ZONE).plusSeconds(seconds));
    }

    public static boolean isNowAfter(LocalDateTime time) {
        return nowUtc().isAfter(time.toInstant(UTC_ZONE));
    }

    public static boolean isNowAfter(LocalDateTime time, int seconds) {
        return nowUtc().isAfter(time.toInstant(UTC_ZONE).plusSeconds(seconds));
    }

    public static boolean isAfter(LocalDateTime time1, LocalDateTime time2) {
        return time1.toInstant(UTC_ZONE).isAfter(time2.toInstant(UTC_ZONE));
    }
    public static boolean isNowBetween(LocalDateTime start, LocalDateTime end) {
        return isNowAfter(start) && isNowBefore(end);
    }

    public static boolean isMonthJustBeginning() {
        LocalDateTime now = Times.nowUtcDateTime();
        return now.getDayOfMonth() == 1 && now.getHour() < 8;
    }

    public static boolean isCurrentMonth(LocalDateTime time) {

        // return YearMonth.from(time.toInstant(LOCAL_ZONE)).equals(YearMonth.nowUtc());
        return time.getMonthValue() == Times.nowUtcDateTime().getMonthValue();
    }

    public static int getMonthValue(LocalDateTime time) {

        // return YearMonth.from(time.toInstant(LOCAL_ZONE)).equals(YearMonth.nowUtc());
        return time.getMonthValue();
    }

    public static int getYear(LocalDateTime time) {

        // return YearMonth.from(time.toInstant(LOCAL_ZONE)).equals(YearMonth.nowUtc());
        return time.getYear();
    }

    public static void main(String[] args) {
        LocalDateTime localDateTime = nowUtcDateTime();
        System.out.println(localDateTime);
        System.out.println(Times.utcToBj(localDateTime));
        System.out.println(daysDuration(Times.utcToBj(localDateTime),Times.parse("2019-07-31 00:00:00")));
        //
        // System.out.println(convertUtc2other(LocalDateTime.nowUtc(), ZoneId.of("+9")));
        //
        // System.out.println(convertBj2utc(LocalDateTime.nowUtc()));

    }

}
