package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.RuntimeStatsInfo;
import romever.scan.oasisscan.entity.RuntimeStatsType;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuntimeStatsInfoRepository extends JpaRepository<RuntimeStatsInfo, Integer>,
        JpaSpecificationExecutor<RuntimeStatsInfo> {
    Optional<RuntimeStatsInfo> findByRuntimeIdAndEntityIdAndStatsType(String runtimeId, String entityId, RuntimeStatsType statsType);

    List<RuntimeStatsInfo> findByRuntimeId(String runtimeId);

    @Query(value = "select distinct entity_id from runtime_stats_info where runtime_id=?1", nativeQuery = true)
    List<String> entities(String runtimeId);
}
