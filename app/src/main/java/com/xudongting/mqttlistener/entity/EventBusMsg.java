package com.xudongting.mqttlistener.entity;

/**
 * Created by xudongting on 2018/5/20.
 */

public class EventBusMsg {
    private String msg;

    public EventBusMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
