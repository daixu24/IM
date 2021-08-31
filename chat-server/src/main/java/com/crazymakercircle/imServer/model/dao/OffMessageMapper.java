package com.crazymakercircle.imServer.model.dao;

import com.crazymakercircle.imServer.model.OffMessage;

import java.util.List;

public interface OffMessageMapper {
    int deleteByPrimaryKey(String mesId);

    int insert(OffMessage record);

    int insertSelective(OffMessage record);

    OffMessage selectByPrimaryKey(String mesId);

    int updateByPrimaryKeySelective(OffMessage record);

    int updateByPrimaryKey(OffMessage record);

    List<OffMessage> selectByUid(String uid);
}