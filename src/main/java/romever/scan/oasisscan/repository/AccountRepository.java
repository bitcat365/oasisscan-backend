package romever.scan.oasisscan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.Account;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>,
        JpaSpecificationExecutor<Account> {

    Optional<Account> findByAddress(String address);

    List<Account> findByOrderByTotalDesc(Pageable pageable);

    long countBy();
}
