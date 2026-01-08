package server.service;

import common.DailyStat;
import server.dao.DailyStatsDAO;

import java.time.LocalDate;
import java.util.List;

public class CityReportGenerator implements ReportGenerator {
    @Override
    public List<DailyStat> generate(LocalDate from, LocalDate to, Integer cityId) {
        if (cityId == null) {
            throw new IllegalArgumentException("City ID required for City Report");
        }
        return DailyStatsDAO.getStats(from, to, cityId);
    }
}
