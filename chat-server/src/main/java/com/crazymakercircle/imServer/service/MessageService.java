package com.crazymakercircle.imServer.service;

import com.crazymakercircle.imServer.model.Message;

public interface MessageService {


    //往数据库插入消息 不需要太多操作
    public void add(Message message);


}
