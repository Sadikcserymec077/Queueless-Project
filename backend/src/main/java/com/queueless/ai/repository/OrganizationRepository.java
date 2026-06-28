package com.queueless.ai.repository;

import com.queueless.ai.entity.Organization;
import com.queueless.ai.entity.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Query("""
            select o from Organization o
            where (cast(:q as String) is null or lower(o.name) like lower(concat('%', cast(:q as String), '%')))
              and (:type is null or o.type = :type)
              and (:includeInactive = true or o.active = true)
            """)
    Page<Organization> search(
            @Param("q") String q,
            @Param("type") OrganizationType type,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable
    );
}
