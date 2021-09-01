package com.crazymakercircle.imServer.rabbitMQ;

import com.crazymakercircle.imServer.service.OffMessageService;
import com.crazymakercircle.imServer.model.*;
import com.crazymakercircle.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by jiangyunxiong on 2018/5/29.
 */
@Service
public class MQReceiver {

    private static Logger log = LoggerFactory.getLogger(MQReceiver.class);


    @Autowired
    OffMessageService offMessageService;


    @RabbitListener(queues=MQConfig.QUEUE)
    public void receive(String message){
        log.info("消费者开始处理消息");
        log.info("receive message:"+message);
        OffMessage offMessage = JsonUtil.jsonToPojo(message, OffMessage.class);
        offMessageService.add(offMessage);

    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
    public void receiveTopic1(String message) {
        log.info(" topic  queue1 message:" + message);
    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
    public void receiveTopic2(String message) {
        log.info(" topic  queue2 message:" + message);
    }
}
