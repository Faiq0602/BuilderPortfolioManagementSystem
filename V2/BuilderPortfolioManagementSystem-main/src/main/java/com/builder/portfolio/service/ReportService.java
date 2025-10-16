package com.builder.portfolio.service;

import com.builder.portfolio.model.PortfolioReport;
import java.util.List;

public interface ReportService extends AutoCloseable {
    PortfolioReport generatePortfolioReportParallel(List<Long> projectIds);

    PortfolioReport generatePortfolioReportSequential(List<Long> projectIds);

    @Override
    void close();
}
