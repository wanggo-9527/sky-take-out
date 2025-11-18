package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.LoginService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private JwtProperties jwtProperties;
    @PostMapping("/user/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("微信用户登录：{}",userLoginDTO);
        User user = loginService.login(userLoginDTO);
        //生成登录令牌
        Map<String,Object> claims = new HashMap<>();
        claims.put("empId",user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),jwtProperties.getUserTtl(),claims);
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();
        return Result.success(userLoginVO);

    }
}
