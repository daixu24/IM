package com.crazymakercircle.imServer.serverProcesser;

import com.crazymakercircle.im.common.bean.msg.ProtoMsg;
import com.crazymakercircle.imServer.model.OffMessage;
import com.crazymakercircle.imServer.server.session.LocalSession;
import com.crazymakercircle.imServer.server.session.ServerSession;
import com.crazymakercircle.imServer.server.session.service.SessionManger;
import com.crazymakercircle.util.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.crazymakercircle.imServer.rabbitMQ.MQSender;

import java.util.List;

@Slf4j
@Service("ChatRedirectProcesser")
public class ChatRedirectProcesser extends AbstractServerProcesser {

    public static final int RE_DIRECT = 1;
    @Autowired
    MQSender mqSender;

    @Override
    public ProtoMsg.HeadType op() {
        return ProtoMsg.HeadType.MESSAGE_REQUEST;
    }

    @Override
    public Boolean action(LocalSession fromSession, ProtoMsg.Message proto) {
        // 聊天处理
        ProtoMsg.MessageRequest messageRequest = proto.getMessageRequest();
        Logger.tcfo("chatMsg | from="
                + messageRequest.getFrom()
                + " , to =" + messageRequest.getTo()
                + " , MsgType =" + messageRequest.getMsgType()
                + " , content =" + messageRequest.getContent());

        // 获取接收方的chatID
        String to = messageRequest.getTo();
        // int platform = messageRequest.getPlatform();
        List<ServerSession> toSessions = SessionManger.inst().getSessionsBy(to);

        //目前版本  先将离线消息存下来即可， 在线的话就不存了，直接发送，下个版本才开发历史消息库
        if (toSessions == null) {
            //接收方离线  给MQ发送消息   存储即可
            log.info("开始使用MQ保存到数据库中");
            mqSender.sendSImMessage(new OffMessage(String.valueOf(messageRequest.getMsgId()), messageRequest.getTo(), messageRequest.getFrom(),messageRequest.getContent()));
            Logger.tcfo("[" + to + "] 不在线，已经保存到数据库中 等待用户拉取!");

        } else {

            toSessions.forEach((session) ->
            {
                // 将IM消息发送到接收客户端；
                // 如果是remotesession，则转发到对应的服务节点
                session.writeAndFlush(proto);

            });
        }
        return null;
    }

}
