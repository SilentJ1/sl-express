package com.sl.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import com.itheima.auth.factory.AuthTemplateFactory;
import com.itheima.auth.sdk.AuthTemplate;
import com.itheima.auth.sdk.dto.AuthUserInfoDTO;
import com.itheima.auth.sdk.service.TokenCheckService;
import com.sl.gateway.config.MyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 司机端token拦截处理
 */
@Component
@Slf4j
public class DriverTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements AuthFilter {

    @Resource
    private MyConfig myConfig;

    @Resource
    private TokenCheckService tokenCheckService;

    @Value("${role.Driver}")
    List<Long> roleDriverIds;

    @Override
    public GatewayFilter apply(Object config) {
        return new TokenGatewayFilter(this.myConfig, this);
    }

    @Override
    public AuthUserInfoDTO check(String token) {
        //校验token
        return this.tokenCheckService.parserToken(token);
    }

    @Override
    public Boolean auth(String token, AuthUserInfoDTO authUserInfoDTO, String path) {

        //通过AuthTemplateFactory的.get(token)方法构造AuthTemplate对象，用来获取用户角色信息
        AuthTemplate authTemplate = AuthTemplateFactory.get(token);
        //此处传来的？？AuthUserInfoDTO信息哪里得到，TokenGateway和DriverTokenGateway的调用顺序
        Long userId = authUserInfoDTO.getUserId();
        //获取userId对应的角色信息
        //.findRoleByUserId(userId)方法返回的时Result<List<Long>>对象，要用.getData获得数据
        List<Long> roleIds = authTemplate.opsForRole().findRoleByUserId(userId).getData();
        //使用工具CollUtil.intersection测试是否有交集
        Collection<Long> intersection = CollUtil.intersection(roleIds, roleDriverIds);
        return CollUtil.isNotEmpty(intersection);
    }
}
