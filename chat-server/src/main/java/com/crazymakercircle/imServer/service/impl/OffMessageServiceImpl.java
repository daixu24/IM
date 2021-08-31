package com.crazymakercircle.imServer.service.impl;


import com.crazymakercircle.imServer.model.OffMessage;
import com.crazymakercircle.imServer.model.dao.*;
import com.crazymakercircle.imServer.service.OffMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OffMessageServiceImpl implements OffMessageService {


    @Autowired
    OffMessageMapper offMessageMapper;

    @Override
    public void add(OffMessage offMessage) {

        OffMessage msg = offMessageMapper.selectByPrimaryKey(offMessage.getMesId());
        if(msg == null){

            offMessageMapper.insert(offMessage);
        }
        else{

            log.info("重复发送");
        }

    }

    @Override
    public void delete(String msgId) {

        int count = offMessageMapper.deleteByPrimaryKey(msgId);
        if(count == 0){
            log.info("删除失败或者没有该条记录");
            return;
        }
        log.info("删除离线消息成功");

    }

    //使用uid来获取所有离线消息
    @Override
    public List<OffMessage> getMessagesById(String uid) {

        List<OffMessage> list = offMessageMapper.selectByUid(uid);

        return list;
    }


    //模拟了关键的字段，并没有将全部字段存储  以后改进会
    //使用消息幂等性进行存储


}
