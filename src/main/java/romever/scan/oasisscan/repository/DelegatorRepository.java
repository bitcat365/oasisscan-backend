package romever.scan.oasisscan.repository;

import org.hibernate.annotations.SortComparator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import romever.scan.oasisscan.entity.Delegator;

import java.util.List;
import java.util.Optional;

@Repository
public interface DelegatorRepository extends JpaRepository<Delegator, Integer>,
        JpaSpecificationExecutor<Delegator> {
    Optional<Delegator> findByValidatorAndDelegator(String validator, String delegator);

    @SortComparator(SharesComparator.class)
    Page<Delegator> findByValidator(String address, Pageable pageable);

    long countByValidator(String validator);

    @Query(value = "select count(distinct delegator) from delegator", nativeQuery = true)
    long countDistinctDelegator();

    @Query(value = "select convert(sum(shares),decimal(22,0)) from delegator where delegator=?1", nativeQuery = true)
    String sumDelegatorEscrow(String delegator);

    List<Delegator> findByDelegator(String address);

    @Query(value = "select d.* from delegator d left join validator_info v on d.validator=v.entity_address where v.entity_address is not null and d.delegator=?1 order by CAST(d.shares as UNSIGNED) desc",
            countQuery = "select count(d.delegator) from delegator d left join validator_info v on d.validator=v.entity_address where v.entity_address is not null and d.delegator=?1",
            nativeQuery = true)
    Page<Delegator> findByDelegatorAll(String delegator, Pageable pageable);

    Page<Delegator> findByDelegator(String delegator, Pageable pageable);
}
