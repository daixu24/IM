package com.crazymakercircle.imServer.model.dao;

import com.crazymakercircle.imServer.model.Message;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageMapper {
    int deleteByPrimaryKey(String mesId);

    int insert(Message record);

    int insertSelective(Message record);

    Message selectByPrimaryKey(String mesId);

    int updateByPrimaryKeySelective(Message record);

    int updateByPrimaryKey(Message record);
}