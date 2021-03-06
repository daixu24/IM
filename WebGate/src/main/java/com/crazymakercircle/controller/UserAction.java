package com.crazymakercircle.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.crazymakercircle.Balance.ImLoadBalance;
import com.crazymakercircle.controller.utility.BaseController;
import com.crazymakercircle.entity.ImNode;
import com.crazymakercircle.entity.LoginBack;
import com.crazymakercircle.im.common.bean.UserDTO;
import com.crazymakercircle.mybatis.entity.UserPO;
import com.crazymakercircle.service.UserService;
import com.crazymakercircle.util.JsonUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * WEB GATE

 */

//@EnableAutoConfiguration
@RestController
@RequestMapping(value = "/user", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api("User 相关的api")
public class UserAction extends BaseController
{
    @Resource
    private UserService userService;
    @Resource
    private ImLoadBalance imLoadBalance;

    /**
     * Web短连接登录
     *
     * @param username 用户名
     * @param password 命名
     * @return 登录结果
     */


    //这里模拟了一个登陆的过程
    @ApiOperation(value = "登录", notes = "根据用户信息登录")
    @RequestMapping(value = "/login/{username}/{password}", method = RequestMethod.GET)
    public String loginAction(
            @PathVariable("username") String username,
            @PathVariable("password") String password)
    {
        UserPO user = new UserPO();
        user.setUserName(username);
        user.setPassWord(password);
        user.setUserId(user.getUserName());

//        User loginUser = userService.login(user);


        LoginBack back = new LoginBack();
        /**
         * 取得最佳的Netty服务器-->取得所有的节点
         */
        //ImNode bestWorker = imLoadBalance.getBestWorker();
        //back.setImNode(bestWorker);
        List<ImNode> allWorker = imLoadBalance.getWorkers();
        //设置节点集合
        back.setImNodeList(allWorker);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        back.setUserDTO(userDTO);
        back.setToken(user.getUserId().toString());
        String r = JsonUtil.pojoToJson(back);
        //返回beack   提供给用户去选择哪个节点  只是起到一个登陆作用而已
        return r;
    }


    /**
     * 从zookeeper中删除所有IM节点
     *
     * @return 删除结果
     */
    @ApiOperation(value = "删除节点", notes = "从zookeeper中删除所有IM节点")
    @RequestMapping(value = "/removeWorkers", method = RequestMethod.GET)
    public String removeWorkers()
    {
        imLoadBalance.removeWorkers();
        return "已经删除";
    }

}