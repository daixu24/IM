package com.crazymakercircle.imServer.server.session.dao;


import com.crazymakercircle.imServer.server.session.entity.SessionCache;

/**
 * 会话管理  DAO

 **/
public interface SessionCacheDAO
{
    //保存 会话 到  缓存
    void save(SessionCache s);

    //从缓存 获取  会话
    SessionCache get(String sessionId);

    //删除会话
    void remove(String sessionId);

}
