package com.xudongting.mqttlistener.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xudongting.mqttlistener.R;
import com.xudongting.mqttlistener.receiver.BootReceiver;
import com.xudongting.mqttlistener.service.MqttService;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ddd";
    @BindView(R.id.host)
    TextView host;
    @BindView(R.id.port)
    TextView port;
    @BindView(R.id.uesrName)
    TextView userName;
    @BindView(R.id.password)
    TextView password;
    @BindView(R.id.topic)
    TextView topic;
    @BindView(R.id.btn_start)
    Button btnStart;
    @BindView(R.id.btn_stop)
    Button btnStop;
    private String textHost, textPort, textUserName, textPassword, textTopic;
    private boolean isChange = false;
    private boolean isReady = true;
    private BootReceiver bootReceiver;
    private IntentFilter intentFilter;

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

    }

    //检查用户输入
    private void checkInput() {
        if (!(textHost.equals("") && textPort.equals("") && textUserName.equals("") && textPassword.equals("") && textTopic.equals(""))) {
            isChange = true;
            if (textHost.equals("") || textPort.equals("") || textUserName.equals("") || textPassword.equals("") || textTopic.equals("")) {
                isReady = false;
            }
        }

    }

    //获取输入框内容
    private void initData() {
        textHost = host.getText().toString();
        textPort = port.getText().toString();
        textUserName = userName.getText().toString();
        textPassword = password.getText().toString();
        textTopic = topic.getText().toString();
        Log.d(TAG, "initData: " + textHost);

    }

    private void setListener() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isChange = false;
                isReady = true;
                initData();
                checkInput();
                registerReceiver(bootReceiver, intentFilter);
                if (isReady == false) {
                    Toast.makeText(MainActivity.this, "请输入完整配置信息！", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChange == false) {
                        Intent it = new Intent(MainActivity.this, MqttService.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("host", "120.78.209.207");
                        bundle.putString("port", "1883");
                        bundle.putString("userName", "listener");
                        bundle.putString("password", "lguning");
                        bundle.putString("topic", "testTopic");
                        it.putExtras(bundle);
                        startService(it);
                        Toast.makeText(MainActivity.this,"服务开启",Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this,"服务开启！",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, MqttService.class);
                stopService(it);
                registerReceiver(bootReceiver, intentFilter);
                Toast.makeText(MainActivity.this,"服务停止！",Toast.LENGTH_SHORT).show();
            }
        });
    }

}
