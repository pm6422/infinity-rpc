package org.infinity.luix.webcenter.repository;

import org.infinity.luix.webcenter.domain.AuthorityAdminMenu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the AuthorityAdminMenu entity.
 */
@Repository
public interface AuthorityAdminMenuRepository extends MongoRepository<AuthorityAdminMenu, String> {

    List<AuthorityAdminMenu> findByAuthorityNameIn(List<String> authorityNames);

    void deleteByAuthorityNameAndAdminMenuIdIn(String authorityName, List<String> adminMenuIds);
}
