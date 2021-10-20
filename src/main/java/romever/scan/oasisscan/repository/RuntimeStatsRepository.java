package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.RuntimeStats;
import romever.scan.oasisscan.entity.RuntimeStatsCount;
import romever.scan.oasisscan.entity.RuntimeStatsType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RuntimeStatsRepository extends JpaRepository<RuntimeStats, Integer>,
        JpaSpecificationExecutor<RuntimeStats> {
    Optional<RuntimeStats> findFirstByRuntimeIdOrderByIdDesc(String runtimeId);

    Optional<RuntimeStats> findByRuntimeIdAndEntityIdAndRoundAndStatsType(String runtimeId, String nodId, long round, RuntimeStatsType statsType);

    @Query(value = "select stats_type as type,count(id) as count from runtime_stats where runtime_id=?1 and entity_id=?2 group by stats_type", nativeQuery = true)
    List statsByRuntimeId(String runtimeId, String entityId);

    @Query(value = "select distinct entity_id from runtime_stats where runtime_id=?1", nativeQuery = true)
    List<String> entities(String runtimeId);

}
