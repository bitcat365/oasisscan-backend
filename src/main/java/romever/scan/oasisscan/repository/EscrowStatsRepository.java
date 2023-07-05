package romever.scan.oasisscan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.EscrowStats;
import romever.scan.oasisscan.vo.response.ChartResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowStatsRepository extends JpaRepository<EscrowStats, Long>,
        JpaSpecificationExecutor<EscrowStats> {
    Optional<EscrowStats> findFirstByOrderByHeightDesc();

    Optional<EscrowStats> findByEntityAddressAndHeight(String address, long height);

    List<EscrowStats> findByEntityAddressOrderByHeightDesc(String address, Pageable pageable);

    @Query(value = "select sum(cast(`escrow` as signed)) as 'value', date as 'key' from escrow_stats group by date order by date desc limit ?1", nativeQuery = true)
    List<ChartResponse> escrowTotalStats(int limit);
}
