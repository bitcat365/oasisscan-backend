package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.RuntimeNode;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuntimeNodeRepository extends JpaRepository<RuntimeNode, Integer>,
        JpaSpecificationExecutor<RuntimeNode> {

    Optional<RuntimeNode> findByRuntimeIdAndNodeId(String runtimeId, String nodeId);

    @Query(value = "select distinct entity_id from runtime_node where runtime_id=?1", nativeQuery = true)
    List<String> entities(String runtimeId);
}
