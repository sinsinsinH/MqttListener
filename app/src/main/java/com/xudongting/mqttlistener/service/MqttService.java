package com.xudongting.mqttlistener.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.xudongting.mqttlistener.activity.NotifyActivity;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

public class MqttService extends Service {
    private String host, userName, password, topic;
    private int port;
    private static final String TAG = "ddd";

    public MqttService() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + "开始服务");
        //服务自启从sharedPrefenences读取配置信息
        if (intent.getExtras()!=null){
            host = intent.getExtras().getString("host");
            port = Integer.valueOf(intent.getExtras().getString("port"));
            userName = intent.getExtras().getString("userName");
            password = intent.getExtras().getString("password");
            topic = intent.getExtras().getString("topic");
            SharedPreferences sp = getSharedPreferences("data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("host", host);
            editor.putInt("port", port);
            editor.putString("userName", userName);
            editor.putString("password", password);
            editor.putString("topic", topic);
            editor.commit();
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                startMQTT(getSharedPreferences("data", Context.MODE_PRIVATE).getString("topic", ""));
            }
        }.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: " + "停止服务");
    }

    //监听mqtt
    public Message startMQTT(String topic) {
        Message message = null;
        String str = null;
        MQTT mqtt = new MQTT();
        try {
            mqtt.setHost(getSharedPreferences("data", Context.MODE_PRIVATE).getString("host", ""), getSharedPreferences("data", Context.MODE_PRIVATE).getInt("port", 0));
            mqtt.setUserName(getSharedPreferences("data", Context.MODE_PRIVATE).getString("userName", ""));
            mqtt.setPassword(getSharedPreferences("data", Context.MODE_PRIVATE).getString("password", ""));
            mqtt.setKeepAlive((short) 0);
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();
            Topic[] topics = {new Topic(topic, QoS.AT_LEAST_ONCE)};
            byte[] qoses = connection.subscribe(topics);

            while (true) {
                message = connection.receive();
                message.ack();
                str = new String(message.getPayload());
                Log.d(TAG, "MqttListener: " + str);
                sendNotification(str);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return message;
    }

    //发送推送消息方法
    public void sendNotification(String str) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("MqttDemo");//设置通知标题
        builder.setContentText(str);//设置通知内容
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);//设置通知的方式
        builder.setAutoCancel(true);//点击通知后，状态栏自动删除通知
        builder.setSmallIcon(android.R.drawable.ic_dialog_alert);//设置小图标
        builder.setContentIntent(PendingIntent.getActivity(this, 0x102, new Intent(this, NotifyActivity.class), 0));//设置点击通知后将要启动的程序组件对应的PendingIntent

        Notification notification = builder.build();

        //发送通知
        notificationManager.notify(0x101, notification);
    }
}
