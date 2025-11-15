package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    @ApiOperation("文件上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("文件上传：{}", file.getOriginalFilename());
        //换个文件名
        //获取扩展 名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //uuid
        String fileName = UUID.randomUUID() + suffix;
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = dir + "/" + fileName;
        //交给阿里云
        String url = aliOssUtil.upload(file.getBytes(), objectName);
        return Result.success(url);
    }



}
