package org.infinity.rpc.appclient.repository;

import org.infinity.rpc.appclient.domain.DictItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DictItem entity.
 */
@Repository
public interface DictItemRepository extends MongoRepository<DictItem, String> {

    Page<DictItem> findByDictCodeAndDictItemName(Pageable pageable, String dictCode, String dictItemName);

    List<DictItem> findByDictCode(String dictCode);

    List<DictItem> findByDictCodeAndDictItemCode(String dictCode, String dictItemCode);

    Page<DictItem> findByDictCode(Pageable pageable, String dictCode);

    Page<DictItem> findByDictItemName(Pageable pageable, String dictItemName);
}
