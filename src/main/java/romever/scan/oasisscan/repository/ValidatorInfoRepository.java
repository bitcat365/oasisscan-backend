package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import romever.scan.oasisscan.entity.ValidatorInfo;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidatorInfoRepository extends JpaRepository<ValidatorInfo, Integer>,
        JpaSpecificationExecutor<ValidatorInfo> {

    List<ValidatorInfo> findByOrderByEscrowDesc();

    Optional<ValidatorInfo> findByEntityId(String entityId);

    Optional<ValidatorInfo> findByNodeId(String nodeId);

    Optional<ValidatorInfo> findByEntityAddress(String entityAddress);

    Optional<ValidatorInfo> findByNodeAddress(String nodeAddress);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update validator_info set `nodes` = 0, signs=0, proposals=0, signs_uptime=0,escrow_24h='0'", nativeQuery = true)
    int updateValidatorStatus();

}
