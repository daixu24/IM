package com.crazymakercircle.imServer.serverProcesser;

import com.crazymakercircle.im.common.ProtoInstant;
import com.crazymakercircle.im.common.bean.ChatMsg;
import com.crazymakercircle.im.common.bean.UserDTO;
import com.crazymakercircle.im.common.bean.msg.ProtoMsg;
import com.crazymakercircle.imServer.model.OffMessage;
import com.crazymakercircle.imServer.protoBuilder.LoginResponceBuilder;
import com.crazymakercircle.imServer.server.session.LocalSession;
import com.crazymakercircle.imServer.server.session.service.SessionManger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.crazymakercircle.imServer.service.*;
import com.crazymakercircle.imServer.protoBuilder.*;

import java.util.List;

@Data
@Slf4j
@Service("LoginProcesser")
public class LoginProcesser extends AbstractServerProcesser
{
    @Autowired
    LoginResponceBuilder loginResponceBuilder;
    @Autowired
    SessionManger sessionManger;

    @Autowired
    OffMessageService offMessageService;

    @Override
    public ProtoMsg.HeadType op()
    {
        return ProtoMsg.HeadType.LOGIN_REQUEST;
    }

    @Override
    public Boolean action(LocalSession session,
                          ProtoMsg.Message proto)
    {
        // 取出token验证
        ProtoMsg.LoginRequest info = proto.getLoginRequest();
        long seqNo = proto.getSequence();

        UserDTO user = UserDTO.fromMsg(info);

        //检查用户
        boolean isValidUser = checkUser(user);
        if (!isValidUser)
        {
            ProtoInstant.ResultCodeEnum resultcode =
                    ProtoInstant.ResultCodeEnum.NO_TOKEN;
            ProtoMsg.Message response =
                    loginResponceBuilder.loginResponce(resultcode, seqNo, "-1");
            //发送之后，断开连接
            session.writeAndClose(response);
            return false;
        }

        session.setUser(user);

        /**
         * 绑定session
         */
        session.bind();
        sessionManger.addLocalSession(session);


        /**
         * 通知客户端：登录成功
         */


        ProtoInstant.ResultCodeEnum resultcode = ProtoInstant.ResultCodeEnum.SUCCESS;
        ProtoMsg.Message response =
                loginResponceBuilder.loginResponce(resultcode, seqNo, session.getSessionId());
        session.writeAndFlush(response);
        List<OffMessage> list = offMessageService.getMessagesById(user.getUserId());
        log.info("用户登录：开始发送离线消息");
        for(int i = 0; i < list.size(); i++){

            OffMessage of = list.get(i);
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setContent(of.getContent());
            chatMsg.setMsgType(ChatMsg.MSGTYPE.TEXT);
            chatMsg.setTo(of.getToId());
            chatMsg.setFrom(of.getFromId());

            chatMsg.setTime(System.currentTimeMillis());
            chatMsg.setFromNick("nick");

            //保存的是string  设置的是long
            chatMsg.setMsgId(Long.valueOf(of.getMesId()));
            ProtoMsg.Message message =
                    ChatMsgBuilder.buildChatRequest(-1, session, chatMsg);

//        commandClient.waitCommandThread();
            session.writeAndFlush(message);
            //发送完后 数据库删除即可   本应该是放到消息队列中 这里直接删除
            offMessageService.delete(of.getMesId());
        }
//        发送离线消息完成





        return true;
    }

    private boolean checkUser(UserDTO user)
    {

        //校验用户,比较耗时的操作,需要100 ms以上的时间
        //方法1：调用远程用户restfull 校验服务
        //方法2：调用数据库接口校验

 //       List<ServerSession> l = sessionManger.getSessionsBy(user.getUserId());
//
//
//        if (null != l && l.size() > 0)
//        {
//            return false;
//        }

        return true;

    }

}
