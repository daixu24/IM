package com.crazymakercircle.imServer.rabbitMQ;

import com.crazymakercircle.imServer.model.*;
import com.crazymakercircle.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.crazymakercircle.util.JsonUtil;


/**
 * Created by jiangyunxiong on 2018/5/29.
 */
@Service
public class MQSender {

    private static Logger log = LoggerFactory.getLogger(MQSender.class);

    @Autowired
    AmqpTemplate amqpTemplate;

//    public void send(Object message){
//        String msg = RedisService.beanToString(message);
//        log.info("send message:"+msg);
//        amqpTemplate.convertAndSend(MQConfig.QUEUE, message);
//    }

    	public void sendTopic(Object message) {
		//String msg = RedisService.beanToString(message);

		String msg	= JsonUtil.pojoToJson(message);
		log.info("send topic message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key1", msg+"1");
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key2", msg+"2");
	}

	public void sendSImMessage(OffMessage message){
    	    log.info("发送消息到消息队列");
        String msg	= JsonUtil.pojoToJson(message);
        log.info("send message:"+msg);
        amqpTemplate.convertAndSend(MQConfig.QUEUE, msg);

    }
}
