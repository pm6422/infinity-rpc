package org.infinity.rpc.appclient.dto;

import io.swagger.annotations.ApiModel;
import org.infinity.rpc.appclient.domain.MongoOAuth2RefreshToken;

@ApiModel("刷新令牌信息DTO")
public class MongoOAuth2RefreshTokenDTO extends MongoOAuth2RefreshToken {

    private static final long serialVersionUID = 1L;

}
