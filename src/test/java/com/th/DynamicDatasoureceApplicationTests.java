package com.th;

import com.th.model.User;
import com.th.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class DynamicDatasoureceApplicationTests {

    @Autowired
    UserService userService;

    @Test
    void contextLoads() {

        List<User> allUsers = userService.getAllUsers();
        for (User user : allUsers) {
            System.out.println(user);
        }
    }

}
