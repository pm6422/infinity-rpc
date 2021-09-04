package org.infinity.luix.webcenter.repository;

import org.infinity.luix.webcenter.domain.RpcScheduledTaskLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the RpcScheduledTaskLock entity.
 */
@Repository
public interface RpcScheduledTaskLockRepository extends MongoRepository<RpcScheduledTaskLock, String> {

    Optional<RpcScheduledTaskLock> findByName(String name);

}
