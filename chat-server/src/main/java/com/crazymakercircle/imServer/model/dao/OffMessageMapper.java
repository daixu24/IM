package com.crazymakercircle.imServer.model.dao;

import com.crazymakercircle.imServer.model.OffMessage;

public interface OffMessageMapper {
    int deleteByPrimaryKey(String id);

    int insert(OffMessage record);

    int insertSelective(OffMessage record);

    OffMessage selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(OffMessage record);

    int updateByPrimaryKey(OffMessage record);
}