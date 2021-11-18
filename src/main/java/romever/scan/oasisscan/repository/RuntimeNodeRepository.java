package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.RuntimeNode;

import java.util.Optional;

@Repository
public interface RuntimeNodeRepository extends JpaRepository<RuntimeNode, Integer>,
        JpaSpecificationExecutor<RuntimeNode> {

    Optional<RuntimeNode> findByRuntimeIdAndNodeId(String runtimeId, String nodeId);
}
