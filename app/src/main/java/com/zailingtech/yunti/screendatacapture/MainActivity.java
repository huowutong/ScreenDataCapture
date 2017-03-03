package com.zailingtech.yunti.screendatacapture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv01;
    private String screenID = "000000"; //默认ID
    private Button btn_start;
    private Button btn_stop;
    private Button btn_upload;
    private Button btn_info;
    private String fileName; //包文件名
    private static final int maxNum = 4; //保存抓包数据的最大数量
    private MainActivityPresenter presenter;
    private String rootPath;
    private ActionReceiver actionReceiver;
    private String fileDirName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        registerReceiver();
        initData();
    }

    private void registerReceiver() {
        actionReceiver = new ActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.zailingtech.yunti.CAPTURE_ACTION_START");
        registerReceiver(actionReceiver, filter);
    }

    private void initData() {
        presenter = new MainActivityPresenter(this);
        ArrayList<File> pcapFiles = presenter.getPcapFiles();
        //处理SD卡中的包的数量
        presenter.handlePcapFils(pcapFiles, maxNum);
        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        LogManager.getLogger().e(getIntent().getAction());
    }

    private void initView() {
        tv01 = (TextView) findViewById(R.id.tv01);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_info = (Button) findViewById(R.id.btn_info);
        btn_info.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        String time = sdf.format(date);
        return time;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                btn_start.setEnabled(false);
                startCapture();
                break;
            case R.id.btn_stop:
                CommandsHelper.stopCapture();
                btn_start.setEnabled(true);
                break;
            case R.id.btn_upload:
                presenter.uploadFile();
                break;
            case R.id.btn_info:
                LogManager.getLogger().e("pcap文件夹中数据包详情--后: %s", presenter.getPcapFiles().toString());
                break;
        }
    }

    private void startCapture() {
        btn_start.setEnabled(false);
        fileName = "capture_" + screenID + "_" + getCurrentTime() + ".pcap";
        // 文件在SDcard根目录下的存储路径
        fileDirName = File.separator + getResources().getString(R.string.pcap_dir_name) + File.separator + fileName;
        new Thread() {
            @Override
            public void run() {
                CommandsHelper.startCapture(fileDirName);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommandsHelper.stopCapture();
        unregisterReceiver(actionReceiver);
        presenter.disConnect();
        LogManager.getLogger().e("抓包APP已退出");
    }

    class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }
            screenID = bundle.getString("ID");
            tv01.setText(screenID + " : " + rootPath);
            String action = bundle.getString("action");
            if (action != null) {
                LogManager.getLogger().e("抓包APP收到的指令: %s", action);
                if (action.equals("start")) {
                    startCapture();
                } else if (action.equals("stop")) {
                    btn_start.setEnabled(true);
                    CommandsHelper.stopCapture();
                } else if (action.equals("upload")) {
                    if (CommandsHelper.isCaptruing) {
                        Toast.makeText(MainActivity.this, "不能在抓包过程中上传数据包", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 上传抓包文件
                    presenter.uploadFile();
                } else if (action.equals("exit")) {
                    MainActivity.this.finish();
                }
            }
        }
    }
}
