package com.crazymakercircle.imServer.service;

import com.crazymakercircle.imServer.model.OffMessage;

import java.util.List;

public interface OffMessageService {

    public void add(OffMessage offMessage);
    public void delete(String msgId);
    public List<OffMessage> getMessagesById(String uid);

}
