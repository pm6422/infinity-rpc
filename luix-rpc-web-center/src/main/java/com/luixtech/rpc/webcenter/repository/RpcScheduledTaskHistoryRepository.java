package com.luixtech.rpc.webcenter.repository;

import com.luixtech.rpc.webcenter.domain.RpcScheduledTaskHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the RpcScheduledTaskHistory entity.
 */
@Repository
public interface RpcScheduledTaskHistoryRepository extends MongoRepository<RpcScheduledTaskHistory, String> {

}
