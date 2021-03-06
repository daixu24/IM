package com.crazymakercircle.imServer.server.session;

import com.crazymakercircle.entity.ImNode;
import com.crazymakercircle.imServer.distributed.PeerSender;
import com.crazymakercircle.imServer.distributed.WorkerRouter;
import com.crazymakercircle.imServer.server.session.entity.SessionCache;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;


@Data
@Builder
@AllArgsConstructor
@Slf4j
//remotesession其实就是A与服务器B连接，C与服务器D连接，那么A与C之间通信 就是remote两个sesesionid之间的通信
public class RemoteSession implements ServerSession, Serializable
{
    private static final long serialVersionUID = -400010884211394846L;
    SessionCache cache;

    private boolean valid = true;


    public RemoteSession(SessionCache cache)
    {
        this.cache = cache;
    }

    /**
     * 通过远程节点，转发
     */
    @Override
    public void writeAndFlush(Object pkg)
    {
        ImNode imNode = cache.getImNode();
        long nodeId = imNode.getId();

        //获取转发的  sender
        log.info("33333333");
        PeerSender sender =
                WorkerRouter.getInst().route(nodeId);

        if(null!=sender)
        {log.info("444444");
            sender.writeAndFlush(pkg);
        }
    }

    @Override
    public String getSessionId()
    {
        //委托
        return cache.getSessionId();
    }

    @Override
    public boolean isValid()
    {
        return valid;
    }

    public void setValid(boolean valid)
    {
        this.valid = valid;
    }

    public String getUserId()
    {
        //委托
        return cache.getUserId();
    }
}
