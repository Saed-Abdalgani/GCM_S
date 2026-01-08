package server.service;

import common.DailyStat;
import server.dao.DailyStatsDAO;

import java.time.LocalDate;
import java.util.List;

public class AllCitiesReportGenerator implements ReportGenerator {
    @Override
    public List<DailyStat> generate(LocalDate from, LocalDate to, Integer cityId) {
        // Ignores cityId, fetches global stats per day
        return DailyStatsDAO.getGlobalStatsPerDay(from, to);
    }
}
