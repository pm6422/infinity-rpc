package org.infinity.rpc.appclient.repository;

import org.infinity.rpc.appclient.domain.AppAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the AppAuthority entity.
 */
@Repository
public interface AppAuthorityRepository extends MongoRepository<AppAuthority, String> {

    Page<AppAuthority> findByAuthorityName(Pageable pageable, String authorityName);

    List<AppAuthority> findByAppName(String appName);

    Page<AppAuthority> findByAppName(Pageable pageable, String appName);

    Page<AppAuthority> findByAppNameAndAuthorityName(Pageable pageable, String appName, String authorityName);

    Optional<AppAuthority> findOneByAppNameAndAuthorityName(String appName, String authorityName);

    void deleteByAppName(String name);

}
