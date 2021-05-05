package romever.scan.oasisscan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.SystemProperty;

import java.util.Optional;

@Repository
public interface SystemPropertyRepository extends JpaRepository<SystemProperty, Integer>,
        JpaSpecificationExecutor<SystemProperty> {
    Optional<SystemProperty> findByProperty(String property);
}
