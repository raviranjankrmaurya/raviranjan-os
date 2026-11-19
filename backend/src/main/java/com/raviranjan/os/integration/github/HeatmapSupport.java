package com.raviranjan.os.integration.github;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds contribution calendars from REAL data only (GitHub's public calendar and commits stored in the database).
 * Nothing is generated, shifted between years or padded: a day without contributions has count 0.
 */
public final class HeatmapSupport {

    private HeatmapSupport() { }

    /**
     * One calendar year, January 1st up to the last day that exists (today for the current year).
     *
     * @param apiDayMap         date -> {count, level} from the GitHub contributions API (may be empty)
     * @param commitDateCounts  date -> number of commits stored in the database (may be empty)
     */
    public static List<Map<String, Object>> buildYear(int year, Map<String, Map<String, Object>> apiDayMap,
                                                      Map<String, Integer> commitDateCounts, LocalDate today) {
        List<Map<String, Object>> days = new ArrayList<>(366);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        if (end.isAfter(today)) end = today;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String key = d.toString();
            int count = 0;
            int level = 0;
            Map<String, Object> api = apiDayMap == null ? null : apiDayMap.get(key);
            if (api != null) {
                count = ((Number) api.getOrDefault("count", 0)).intValue();
                level = ((Number) api.getOrDefault("level", 0)).intValue();
            }
            int fromCommits = commitDateCounts == null ? 0 : commitDateCounts.getOrDefault(key, 0);
            if (fromCommits > count) count = fromCommits;
            if (count > 0 && level == 0) level = levelFor(count);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", key);
            day.put("count", count);
            day.put("level", level);
            days.add(day);
        }
        return days;
    }

    public static int levelFor(int count) {
        if (count <= 0) return 0;
        if (count >= 6) return 4;
        if (count >= 4) return 3;
        return count >= 2 ? 2 : 1;
    }

    public static int total(List<Map<String, Object>> days) {
        int sum = 0;
        for (Map<String, Object> d : days) sum += ((Number) d.getOrDefault("count", 0)).intValue();
        return sum;
    }
}
