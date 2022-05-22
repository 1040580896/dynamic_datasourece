package com.th.controller;

import com.th.datasource.DataSourceType;
import com.th.model.User;
import com.th.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 17:18
 **/
@RestController
public class DataSourceController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    UserService userService;

    /**
     * 修改数据源的接口
     */
    @PostMapping("/dstype")
    public void setDsType(String dsType, HttpSession session){

        //将数据源的信息存放到session
        session.setAttribute(DataSourceType.DS_SESSION_KEY,dsType);
        log.info("数据源切换为:{}",dsType);

    }
    @GetMapping("/users")
    public List<User> getAllUsers(){
        return userService.getAllUsers();
    }
}
