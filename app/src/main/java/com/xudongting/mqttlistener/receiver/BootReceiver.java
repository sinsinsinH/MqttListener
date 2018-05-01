package com.xudongting.mqttlistener.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xudongting.mqttlistener.service.MqttService;


/**
 * Created by xudongting on 2018/4/16.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent it=new Intent(context,MqttService.class);
        context.startService(it);
    }
}
