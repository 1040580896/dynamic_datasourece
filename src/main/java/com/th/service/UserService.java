package com.th.service;

import com.th.annotation.DataSource;
import com.th.mapper.UserMapper;
import com.th.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 16:22
 **/

@Service
@DataSource("slave")
public class UserService {

    @Autowired
    UserMapper userMapper;


    //@DataSource("master")
    public List<User> getAllUsers(){
        return userMapper.getAllUsers();
    }

}
