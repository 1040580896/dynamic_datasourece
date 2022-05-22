package com.th.mapper;

import com.th.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author xiaokaixin
 * @Date 2022/5/22 16:20
 * @Version 1.0
 */
@Mapper
public interface UserMapper {

    @Select("select * from user")
    List<User> getAllUsers();
}
