package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.Runtime;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuntimeRepository extends JpaRepository<Runtime, Integer>,
        JpaSpecificationExecutor<Runtime> {
    Optional<Runtime> findByRuntimeId(String runtimeId);

    List<Runtime> findAllByOrderByStartRoundHeightAsc();
}
