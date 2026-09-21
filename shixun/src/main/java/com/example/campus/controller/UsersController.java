package com.example.campus.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campus.entity.Users;
import com.example.campus.service.IUsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author coder
 * @since 2026-09-20
 */
@RestController
@RequestMapping("/user")
public class UsersController {

    @Autowired //自动注入对象
    IUsersService usersService;

    //user新增请求接口  http://localhost:8088/user/save?nickname=lisi123&password=lisi123
    @RequestMapping("/save")
    public Boolean save(Users user){
        //调用业务层的新增保存方法save
        return usersService.save(user);
    }

    //user修改请求接口  http://localhost:8088/user/update?id=22&nickname=李四123
    @RequestMapping("/update")
    public Boolean update(Users user){
        //调用业务层的根据id修改方法update
        return usersService.updateById(user);
    }

    //user删除请求接口  http://localhost:8088/user/remove?id=22
    @RequestMapping("/remove")
    public Boolean remove(Integer id){
        //调用业务层的根据id删除方法remove
        return usersService.removeById(id);
    }

    //user查询请求接口  http://localhost:8088/user/query
    @RequestMapping("/query")
    public List<Users> query(){
        //调用业务层的查询方法list
        List<Users> users = usersService.list(); // 调用 list 方法
        return users;
    }

    //user条件查询请求接口  http://localhost:8088/user/queryWrapper?name=test
    @RequestMapping("/queryWrapper")
    public List<Users> queryWrapper(String name){
        // 假设有一个 QueryWrapper 对象，设置查询条件为name的模糊查询
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        //根据name进行模糊查询
//        queryWrapper.like("nickname", name);
//        queryWrapper.eq("open_id", "Testabcd");
        queryWrapper.like("nickname", name).eq("open_id", "Testabcd");
        //调用业务层的查询方法list，注入queryWrapper
        List<Users> users = usersService.list(queryWrapper); // 调用 list 方法
        return users;
    }

    //user修改Wrapper请求接口  http://localhost:8088/user/updateWrapper?openId=ABC&nickname=testabc&password=ABC123
    @RequestMapping("/updateWrapper")
    public Boolean updateWrapper(Users user){
        //创建条件构造器对象
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        //根据nickname修改open_id和password
        queryWrapper.eq("nickname", user.getNickname());
        //调用业务层的根据条件构造器对象修改方法update
        return usersService.update(user,queryWrapper);
    }

    //user条件删除请求接口  http://localhost:8088/user/removeWrapper?nickname=testabc
    @RequestMapping("/removeWrapper")
    public Boolean removeWrapper(Users user){
        //创建条件构造器对象
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        //根据nickname删除
        queryWrapper.eq("nickname", user.getNickname());
        //调用业务层的根据queryWrapper条件删除方法remove
        return usersService.remove(queryWrapper);
    }

}
