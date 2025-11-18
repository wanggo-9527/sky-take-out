package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDTO {
    //code + appid + secret
    private String code;
    private String appid;
    private String secret;
}
