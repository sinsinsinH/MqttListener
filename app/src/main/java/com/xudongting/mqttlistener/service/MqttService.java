package com.xudongting.mqttlistener.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.xudongting.mqttlistener.activity.NotifyActivity;
import com.xudongting.mqttlistener.entity.EventBusMsg;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MqttService extends Service {
    private String host = "120.78.209.207", userName = "listener", password = "lguning";
    private int port = 1883;
    private String imei;
    private String applicationId = "com.xudongting.mqttlistener";
    private String topics;
    private static final String TAG = "ddd";
    MyThread myThread=new MyThread();

    public MqttService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + "开始服务");
        EventBus.getDefault().post(new EventBusMsg("服务已开启..."));
        EventBus.getDefault().post(new EventBusMsg("连接MQTT服务器中..."));
        myThread.start();
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
        myThread.interrupt();
    }

    //监听mqtt
    public Message startMQTT() {
        Message message = null;
        String str = null;
        MQTT mqtt = new MQTT();
        try {
            mqtt.setHost(host, port);
            mqtt.setUserName(userName);
            mqtt.setPassword(password);
            mqtt.setKeepAlive((short) 0);
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();
            String[] strTpoic = topics.split(",");
            Topic[] topics = new Topic[strTpoic.length];
            for (int i = 0; i < topics.length; i++) {
                topics[i] = new Topic(strTpoic[i].substring(1, strTpoic[i].length() - 1), QoS.AT_LEAST_ONCE);
            }
            byte[] qoses = connection.subscribe(topics);
            EventBus.getDefault().post(new EventBusMsg("服务器连接成功..."));
            EventBus.getDefault().post(new EventBusMsg("开始监听消息..."));
            while (true) {
                message = connection.receive();
                message.ack();
                str = new String(message.getPayload());
                Log.d(TAG, "startMQTT: " + str);
                JSONObject jsonObject = new JSONObject(str);
                String title = jsonObject.getString("title");
                String content = jsonObject.getString("content");
                Log.d(TAG, "startMQTT: "+title+content);
                EventBus.getDefault().post(new EventBusMsg("获取到消息..."));
                EventBus.getDefault().post(new EventBusMsg("title:" + title + "  content:" + content));
                sendNotification(title, content);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return message;
    }

    //发送推送消息方法
    public void sendNotification(String title, String content) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(title);//设置通知标题
        builder.setContentText(content);//设置通知内容
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);//设置通知的方式
        builder.setAutoCancel(true);//点击通知后，状态栏自动删除通知
        builder.setSmallIcon(android.R.drawable.ic_dialog_alert);//设置小图标
        builder.setContentIntent(PendingIntent.getActivity(this, 0x102, new Intent(this, NotifyActivity.class), 0));//设置点击通知后将要启动的程序组件对应的PendingIntent

        Notification notification = builder.build();

        //发送通知
        notificationManager.notify(0x101, notification);
    }

    //获取IMEI
    public void getIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        imei = telephonyManager.getDeviceId();
        Log.d(TAG, "initData: " + imei);
    }
    public class MyThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                URL url = new URL("http://120.78.209.207:8888/api/getTopics" + "?uid=" + imei + "&applicationId=" + applicationId);
                HttpURLConnection coon = (HttpURLConnection) url.openConnection();
                coon.setRequestMethod("GET");
                coon.setReadTimeout(6000);
                InputStream in = coon.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] b = new byte[1024];
                int length = 0;
                while ((length = in.read(b)) > -1) {
                    baos.write(b, 0, length);
                }
                String msg = baos.toString();
                String str = new JSONObject(msg).getJSONObject("data").getJSONArray("topics").toString();
                topics = str.substring(1, str.length() - 1);
                Log.d(TAG, "run: " + topics);

                startMQTT();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        }
    }

