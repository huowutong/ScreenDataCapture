package com.zailingtech.yunti.screendatacapture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CaptureDataListener, UploadListener {

    private TextView tv01;
    private String screenID = "000000"; //默认ID
    private Button btn_start;
    private Button btn_stop;
    private Button btn_upload;
    private Button btn_info;
    private EditText et_stop_time;
    private String fileName; //包文件名
    private static final int maxNum = 4; //保存抓包数据的最大数量 -- tips:如果按时间来管理最大包数量，可以使用包名上的时间
    private static final String autoStopTime = "101600";
    private MainActivityPresenter presenter;
    private String rootPath;
    private ActionReceiver actionReceiver;
    private String fileDirName;
    private boolean isChecked = false;
    private boolean firstUpload = false;
    private boolean hasStartTask = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
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
        CommandsHelper.setOnCaptureDataListener(this);
        ArrayList<File> pcapFiles = presenter.getPcapFiles();
        //处理SD卡中的包的数量
        presenter.handlePcapFils(pcapFiles, maxNum);
        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        LogManager.getLogger().e(getIntent().getAction());

    }

    private boolean checkAndUpload() {
        LogManager.getLogger().e("查询数据库并上传条件: %s %s", isChecked, BaseApplication.getScreenID());
        if (!isChecked && BaseApplication.getScreenID() != null) {
            // 查询数据库，最新的一条数据的是否已上传？
            isChecked = true;
            List<PackageInfo> packageInfos = PackageDao.queryUploadFlag(false, 1);
            LogManager.getLogger().e("APP打开查询数据库: %s", packageInfos.toString());
            if (packageInfos != null && packageInfos.size() > 0) {
                LogManager.getLogger().e("上次有未上传文件,正在上传: %s", packageInfos.get(0).getFileName());
                presenter.uploadFile(packageInfos.get(0).getFileName());
                firstUpload = true;
                return true;
            }
            firstUpload = false;
        }
        return false;
    }

    private void initView() {
        tv01 = (TextView) findViewById(R.id.tv01);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_info = (Button) findViewById(R.id.btn_info);
        et_stop_time = (EditText) findViewById(R.id.et_stop_time);
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
                BaseApplication.setScreenID(screenID);
                startCapture();
                break;
            case R.id.btn_stop:
                CommandsHelper.stopCapture();
                btn_start.setEnabled(true);
                break;
            case R.id.btn_upload:
                presenter.uploadFile(null);
                break;
            case R.id.btn_info:
//                LogManager.getLogger().e("pcap文件夹中数据包详情--后: %s", presenter.getPcapFiles().toString());
                LogManager.getLogger().e("数据库中所有数据: %s", PackageDao.queryAll().toString());
                startTimerTask();
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
                try {
                    CommandsHelper.startCapture(fileDirName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    @Override
    public void onStartCaptrue() {
        //开始抓包后在数据库中插入一条数据
        PackageDao.insert(new PackageInfo(null, fileName, false));
        LogManager.getLogger().e("数据库中所有数据: %s", PackageDao.queryAll().toString());
        //startTimerTask();
    }

    private void startTimerTask() {
        //开启定时任务,在指定时间点停止抓包并上传抓包数据
        try {
            if (!hasStartTask) {
                long period = 24 * 60 * 60 * 1000;
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
                Date date = new Date();
                String time = sdf1.format(date);
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmmss");
                String autoTime = et_stop_time.getText().toString();
                if (TextUtils.isEmpty(autoTime)) {
                    return;
                }
                Date autuoUploadDate = sdf2.parse(time + autoTime);
//                Date autuoUploadDate = sdf2.parse(time + autoStopTime);
                // 如果今天的已经过了 首次运行时间就改为明天
                if (System.currentTimeMillis() > autuoUploadDate.getTime()) {
                    autuoUploadDate = new Date(autuoUploadDate.getTime() + period);
                }
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        LogManager.getLogger().e("定时任务已执行");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btn_start.setEnabled(true);
                            }
                        });
                        CommandsHelper.stopCapture();
                    }
                };
                Timer timer = new Timer();
                // 每24小时执行一次
                timer.scheduleAtFixedRate(task, autuoUploadDate, period);
                LogManager.getLogger().e("定时任务已准备");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStopCaptrue() {
        SystemClock.sleep(1000);
        // 停止抓包后上传抓包文件
        presenter.uploadFile(null);
    }

    @Override
    public void uploadFinish() {
        if (firstUpload) {
            firstUpload = false;
            startCapture();
        }
    }

    class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }
            screenID = bundle.getString("ID");
            if (BaseApplication.getScreenID() == null || !BaseApplication.getScreenID().equals(screenID)) {
                BaseApplication.setScreenID(screenID);
            }
            tv01.setText(screenID + " : " + rootPath);
            String action = bundle.getString("action");
            if (action != null) {
                LogManager.getLogger().e("抓包APP收到的指令: %s", action);
                if (action.equals("start")) {
                    if (!isChecked) {
                        //首次接到广播后，查询数据库并进行上传
                        if (checkAndUpload()) {
                            return;
                        }
                    }
                    startCapture();
                } else if (action.equals("stop")) {
                    btn_start.setEnabled(true);
                    CommandsHelper.stopCapture();
                } else if (action.equals("upload")) {
                    if (CommandsHelper.isCaptruing) {
                        LogManager.getLogger().e("不能在抓包过程中上传数据包");
                        return;
                    }
                    // 上传抓包文件
                    presenter.uploadFile(null);
                } else if (action.equals("clear")) {
                    presenter.clearAllPcapFiles();
                    PackageDao.clearAll(); // 同时清空数据库
                    LogManager.getLogger().e("数据库清空后: %s", PackageDao.queryAll().toString());
                } else if (action.equals("exit")) {
                    MainActivity.this.finish();
                }
            }
        }
    }
}
