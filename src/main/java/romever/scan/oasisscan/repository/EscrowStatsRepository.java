package romever.scan.oasisscan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.EscrowStats;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowStatsRepository extends JpaRepository<EscrowStats, Integer>,
        JpaSpecificationExecutor<EscrowStats> {
    Optional<EscrowStats> findFirstByOrderByHeightDesc();

    Optional<EscrowStats> findByEntityAddressAndHeight(String address, long height);

    List<EscrowStats> findByEntityAddressOrderByHeightDesc(String address, Pageable pageable);
}
