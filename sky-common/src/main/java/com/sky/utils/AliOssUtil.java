package com.sky.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.sky.properties.AliOssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliOssUtil {

    private final AliOssProperties properties;

    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );

        try {
            // 上传文件
            ossClient.putObject(
                    properties.getBucketName(),
                    objectName,
                    new ByteArrayInputStream(bytes)
            );
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 拼接访问路径
        String url = "https://"
                + properties.getBucketName()
                + "."
                + properties.getEndpoint()
                + "/"
                + objectName;

        log.info("文件上传成功: {}", url);
        return url;
    }
}
