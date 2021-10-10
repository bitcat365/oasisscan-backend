package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.RuntimeStats;
import romever.scan.oasisscan.entity.RuntimeStatsType;

import java.util.Optional;

@Repository
public interface RuntimeStatsRepository extends JpaRepository<RuntimeStats, Integer>,
        JpaSpecificationExecutor<RuntimeStats> {
    Optional<RuntimeStats> findFirstByRuntimeIdOrderByHeightDesc(String runtimeId);

    Optional<RuntimeStats> findByRuntimeIdAndEntityIdAndRoundAndStatsType(String runtimeId, String nodId, long round, RuntimeStatsType statsType);
}
