package com.crazymakercircle.imServer.testController;


import com.crazymakercircle.imServer.model.OffMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import  com.crazymakercircle.imServer.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class testDb {

    @Autowired
    OffMessageService offMessageService;

    @ResponseBody
    @GetMapping("/test")
    public List<OffMessage> getDemo(){

        return offMessageService.getMessagesById("z1");

    }


}
