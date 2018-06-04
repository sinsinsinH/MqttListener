package com.xudongting.mqttlistener.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xudongting.mqttlistener.R;
import com.xudongting.mqttlistener.entity.EventBusMsg;
import com.xudongting.mqttlistener.receiver.BootReceiver;
import com.xudongting.mqttlistener.service.MqttService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_start)
    Button btnStart;
    @BindView(R.id.btn_stop)
    Button btnStop;
    @BindView(R.id.listView)
    ListView listView;
    private static final String TAG = "ddd";
    private BootReceiver bootReceiver;
    private IntentFilter intentFilter;
    private ArrayList<String> list = new ArrayList<>();
    private MyAdapter myAdapter;
    private Intent it;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(MainActivity.this);
        ButterKnife.bind(this);
        bootReceiver = new BootReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        myAdapter = new MyAdapter(list, this);
        listView.setAdapter(myAdapter);
        setListener();
        //判断服务是否存活
        serviceIsAlive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(MainActivity.this);
    }

    private void setListener() {
        //开启服务监听事件
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerReceiver(bootReceiver, intentFilter);
                it = new Intent(MainActivity.this, MqttService.class);
                startService(it);
                Toast.makeText(MainActivity.this, "服务开启！", Toast.LENGTH_SHORT).show();
                list.add("服务开启中...");
                myAdapter.notifyDataSetChanged();
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                serviceIsAlive();
            }
        });
        //停止服务监听事件
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                it = new Intent(MainActivity.this, MqttService.class);
                stopService(it);
                Toast.makeText(MainActivity.this, "服务停止！", Toast.LENGTH_SHORT).show();
                list.add("服务关闭...");
                myAdapter.notifyDataSetChanged();
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
            }
        });
    }

    class MyAdapter extends BaseAdapter {
        ArrayList<String> mList;
        Context mContext;

        public MyAdapter(ArrayList<String> list, Context context) {
            this.mList = list;
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.main, null);
            TextView textView = convertView.findViewById(R.id.textView);
            textView.setText(mList.get(position));
            return convertView;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMsg msg) {
        Log.d(TAG, "接收到信息");
        list.add(msg.getMsg());
        myAdapter.notifyDataSetChanged();

    }

    //判断服务是否存活
    public void serviceIsAlive() {
        ActivityManager myAM = (ActivityManager) MainActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = myAM.getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningServices.size(); i++) {
            if (runningServices.get(i).service.getClassName().equals("com.xudongting.mqttlistener.service.MqttService")) {
                btnStart.setEnabled(false);
                list.add("检查到服务已开启...");
                myAdapter.notifyDataSetChanged();
            }
        }
    }

}
