package com.crazymakercircle.imServer.service.impl;


import com.crazymakercircle.imServer.model.Message;
import com.crazymakercircle.imServer.model.dao.*;
import com.crazymakercircle.imServer.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {


    @Autowired
    MessageMapper messageMapper;


    //模拟了关键的字段，并没有将全部字段存储  以后改进会
    //使用消息幂等性进行存储
    @Override
    public void add(Message message) {

        Message msg = messageMapper.selectByPrimaryKey(message.getMesId());
        if(msg == null){

            messageMapper.insert(message);
        }
        else{

            log.info("客户端重发消息，不存储");
        }
    }
}
