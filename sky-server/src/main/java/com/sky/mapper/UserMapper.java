package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);
    @Insert("insert into user (openid,create_time) values (#{openid},#{createTime})")
    void insert(User user);
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 统计指定时间段内新增用户数量
     *  - begin：起始时间（create_time >= begin）
     *  - end：结束时间（create_time <= end）
     */
    Integer countByMap(Map<String, Object> map);
}
