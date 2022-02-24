package romever.scan.oasisscan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.Debonding;

import java.util.List;

@Repository
public interface DebondingRepository extends JpaRepository<Debonding, Integer>,
        JpaSpecificationExecutor<Debonding> {

    @Query(value = "select convert(sum(shares),decimal(22,0)) from debonding where delegator=?1", nativeQuery = true)
    String sumDelegatorDebonding(String delegator);

    List<Debonding> findByDelegatorOrderByDebondEndAsc(String delegator, Pageable pageable);

    long countByDelegator(String delegator);

    long deleteByDelegator(String delegator);
}
