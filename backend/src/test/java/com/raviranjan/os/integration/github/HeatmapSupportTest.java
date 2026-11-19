package com.raviranjan.os.integration.github;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The heatmap must contain real contributions only: no padding, no year shifting. */
class HeatmapSupportTest {

    private static Map<String, Object> day(int count, int level) {
        Map<String, Object> m = new HashMap<>();
        m.put("count", count);
        m.put("level", level);
        return m;
    }

    @Test
    void emptyInputGivesAnAllZeroCalendarNotInventedActivity() {
        List<Map<String, Object>> days = HeatmapSupport.buildYear(2025, new HashMap<>(), new HashMap<>(), LocalDate.of(2026, 3, 1));
        assertEquals(365, days.size());
        assertEquals(0, HeatmapSupport.total(days));
    }

    @Test
    void realApiCountsAreKeptExactly() {
        Map<String, Map<String, Object>> api = new HashMap<>();
        api.put("2025-03-04", day(7, 4));
        api.put("2025-03-05", day(1, 1));
        List<Map<String, Object>> days = HeatmapSupport.buildYear(2025, api, new HashMap<>(), LocalDate.of(2026, 1, 1));
        assertEquals(8, HeatmapSupport.total(days));
    }

    @Test
    void storedCommitsOnlyRaiseTheCountNeverLowerIt() {
        Map<String, Map<String, Object>> api = new HashMap<>();
        api.put("2025-06-01", day(5, 3));
        Map<String, Integer> commits = new HashMap<>();
        commits.put("2025-06-01", 2);     // lower than the API: ignored
        commits.put("2025-06-02", 3);     // not in the API: counted
        List<Map<String, Object>> days = HeatmapSupport.buildYear(2025, api, commits, LocalDate.of(2026, 1, 1));
        assertEquals(8, HeatmapSupport.total(days));
    }

    @Test
    void currentYearStopsAtToday() {
        List<Map<String, Object>> days = HeatmapSupport.buildYear(2026, new HashMap<>(), new HashMap<>(), LocalDate.of(2026, 1, 10));
        assertEquals(10, days.size());
        assertTrue(HeatmapSupport.buildYear(2027, new HashMap<>(), new HashMap<>(), LocalDate.of(2026, 1, 10)).isEmpty());
    }

    @Test
    void levelsFollowTheCount() {
        assertEquals(0, HeatmapSupport.levelFor(0));
        assertEquals(1, HeatmapSupport.levelFor(1));
        assertEquals(2, HeatmapSupport.levelFor(3));
        assertEquals(3, HeatmapSupport.levelFor(4));
        assertEquals(4, HeatmapSupport.levelFor(9));
    }
}
