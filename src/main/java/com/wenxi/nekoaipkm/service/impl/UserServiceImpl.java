package com.wenxi.nekoaipkm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenxi.nekoaipkm.mapper.UserMapper;
import com.wenxi.nekoaipkm.model.entity.User;
import com.wenxi.nekoaipkm.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author kk
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-06-13 01:49:57
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

}




