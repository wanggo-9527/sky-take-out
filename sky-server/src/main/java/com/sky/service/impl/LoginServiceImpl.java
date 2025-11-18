package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.context.BaseContext;
import com.sky.dto.RequestDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.LoginService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.UserLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.sky.context.BaseContext.getCurrentId;

@Service
public class LoginServiceImpl implements LoginService {
    /**
     * 微信登录
     * @param userLoginDTO
     */
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatProperties weChatProperties;


    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String code = userLoginDTO.getCode();
        Map<String, String> claims= new HashMap<>();
        claims.put("appid",weChatProperties.getAppid());
        claims.put("secret",weChatProperties.getSecret());
        claims.put("js_code",code);
        claims.put("grant_type","authorization_code");
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        String json =  httpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session",claims);
        //解析json
        JSONObject jsonObject = JSONObject.parseObject(json);
        String openid = jsonObject.getString("openid");
        User user = userMapper.getByOpenid(openid);
        if(user == null){
            user = User.builder()
                    .createTime(LocalDateTime.now())
                    .openid(openid)
                    .build();
            userMapper.insert(user);
        }
        return  user;
    }

/*    public void login(UserLoginDTO userLoginDTO) {
        String code = userLoginDTO.getCode();
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setCode(code);
        requestDTO.setAppid("wx5c0a0f0c0a0f0c0a");
        requestDTO.setSecret("5c0a0f0c0a0f0c0a");
        //准备发送给微信服务器
        String json = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session", (Map<String, String>) requestDTO);
        System.out.println(json);
        User user = JSONObject.parseObject(json, User.class);


        //取出openid
        String openid = user.getOpenid();
        //查询数据库，判断当前用户是否为新用户
        String openid1 =userMapper.getByOpenid(openid);
        if(openid1 == null){
            userMapper.insert(user);
        }
        //从threadlocal获取token
        Long token = getCurrentId();

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token("123456")
                .build();


    }*/


}
