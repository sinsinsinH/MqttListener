package com.xudongting.mqttlistener.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.xudongting.mqttlistener.R;
import com.xudongting.mqttlistener.receiver.BootReceiver;
import com.xudongting.mqttlistener.service.MqttService;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ddd";
    @BindView(R.id.host)
    EditText host;
    @BindView(R.id.port)
    EditText port;
    @BindView(R.id.uesrName)
    EditText userName;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.topic)
    EditText topic;
    @BindView(R.id.api)
    EditText api;
    @BindView(R.id.btn_start)
    Button btnStart;
    @BindView(R.id.btn_stop)
    Button btnStop;
    @BindView(R.id.btn_get)
    Button btnGet;

    private String textHost, textPort, textUserName, textPassword, textTopic, textApi;
    private boolean isReady = true;
    private BootReceiver bootReceiver;
    private IntentFilter intentFilter;
    private Handler handler;
    private String imei;
    private String applicationId = "com.xudongting.mqttlistener";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        bootReceiver = new BootReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        //设置监听器
        setListener();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    //检查用户输入
    private void checkInput() {
        if (textHost.equals("") || textPort.equals("") || textUserName.equals("") || textPassword.equals("") || textTopic.equals("")) {
            isReady = false;
        } else {
            isReady = true;
        }
    }


    //获取输入框内容
    private void initData() {
        textHost = host.getText().toString();
        textPort = port.getText().toString();
        textUserName = userName.getText().toString();
        textPassword = password.getText().toString();
        textTopic = topic.getText().toString();
        textApi = api.getText().toString();

        //获取IMEI号
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

    private void setListener() {
        //获取topic并显示
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();
                if (textApi.equals("")) {
                    Toast.makeText(MainActivity.this, "请输入api!", Toast.LENGTH_SHORT).show();
                } else {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                URL url = new URL(textApi + "?uid=" + imei + "&applicationId=" + applicationId);
                                HttpURLConnection coon = (HttpURLConnection) url.openConnection();
                                coon.setRequestMethod("GET");
                                coon.setReadTimeout(6000);
                                if (coon.getResponseCode() == 200) {
                                    InputStream in = coon.getInputStream();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte[] b = new byte[1024];
                                    int length = 0;
                                    while ((length = in.read(b)) > -1) {
                                        baos.write(b, 0, length);
                                    }
                                    String msg = baos.toString();
                                    String str = new JSONObject(msg).getJSONObject("data").getJSONArray("topics").toString();
                                    final String topics = str.substring(1, str.length() - 1);
                                    Log.d(TAG, "run: " + topics);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            topic.setText(topics);
                                        }
                                    });

                                } else {
                                    Toast.makeText(MainActivity.this, "请输入正确api！", Toast.LENGTH_SHORT).show();
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        });

        //开启服务监听事件
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();
                checkInput();
                registerReceiver(bootReceiver, intentFilter);
                if (isReady == false) {
                    Toast.makeText(MainActivity.this, "请输入完整配置信息！", Toast.LENGTH_SHORT).show();
                } else {
                    Intent it = new Intent(MainActivity.this, MqttService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("host", textHost);
                    bundle.putString("port", textPort);
                    bundle.putString("userName", textUserName);
                    bundle.putString("password", textPassword);
                    bundle.putString("topic", textTopic);
                    it.putExtras(bundle);
                    startService(it);
                    Toast.makeText(MainActivity.this, "服务开启！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //停止服务监听事件
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, MqttService.class);
                stopService(it);
                registerReceiver(bootReceiver, intentFilter);
                Toast.makeText(MainActivity.this, "服务停止！", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
