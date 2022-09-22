package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.ValidatorConsensus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidatorConsensusRepository extends JpaRepository<ValidatorConsensus, Long>,
        JpaSpecificationExecutor<ValidatorConsensus> {

    Optional<ValidatorConsensus> findByEntityIdAndNodeIdAndConsensusId(String entityId, String nodeId, String consensusId);

    List<ValidatorConsensus> findByEntityId(String entityId);
}
