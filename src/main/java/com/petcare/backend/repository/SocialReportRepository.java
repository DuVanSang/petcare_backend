package com.petcare.backend.repository;

import com.petcare.backend.model.SocialReport;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SocialReportRepository extends JpaRepository<SocialReport, Long>,
        JpaSpecificationExecutor<SocialReport> {
    Optional<SocialReport> findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            Long reporterId,
            SocialReport.ModerationTargetType targetType,
            Long targetId,
            Collection<SocialReport.ReportStatus> statuses
    );
}
