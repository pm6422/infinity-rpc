package org.infinity.rpc.webcenter.repository;

import org.infinity.rpc.webcenter.domain.MongoOAuth2ClientDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the OAuth2AuthenticationClientDetails entity.
 */
@Repository
public interface OAuth2ClientDetailsRepository extends MongoRepository<MongoOAuth2ClientDetails, String> {

}
