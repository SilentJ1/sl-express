package com.sl.gateway.filter;

import cn.hutool.jwt.Claims;
import com.itheima.auth.sdk.dto.AuthUserInfoDTO;
import com.sl.gateway.config.MyConfig;
import com.sl.gateway.properties.JwtProperties;
import com.sl.transport.common.constant.Constants;
import com.sl.transport.common.util.JwtUtils;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 用户端token拦截处理
 */
@Slf4j
@Component
public class CustomerTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements AuthFilter {

    @Resource
    private MyConfig myConfig;

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public GatewayFilter apply(Object config) {
        return new TokenGatewayFilter(this.myConfig, this);
    }

    @Override
    public AuthUserInfoDTO check(String token) {
        Map<String, Object> claims = Jwts.parser().setSigningKey(jwtProperties.getPublicKey().
                getBytes(StandardCharsets.UTF_8)).parseClaimsJws(token).getBody();

        AuthUserInfoDTO authUserInfoDTO = new AuthUserInfoDTO();
        Long userId = Long.valueOf(claims.get("userId").toString());
        authUserInfoDTO.setUserId(userId);
        return authUserInfoDTO;
    }

    @Override
    public Boolean auth(String token, AuthUserInfoDTO authUserInfoDTO, String path) {
        //普通用户不需要校验角色
        return true;
    }

    @Override
    public String tokenHeaderName() {
        return Constants.GATEWAY.ACCESS_TOKEN;
    }
}
