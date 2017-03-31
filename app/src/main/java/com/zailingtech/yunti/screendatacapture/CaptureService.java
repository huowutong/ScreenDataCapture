package com.zailingtech.yunti.screendatacapture;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.zailingtech.yunti.screendatacapture.model.PackageInfo;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Description: 接收到抓包指令后的抓包服务 <br>
 * Author: LUTAO <br>
 * Date: 2017/03/17 <br>
 */

public class CaptureService extends Service implements CaptureDataListener, UploadListener {

    private MainActivityPresenter presenter;
    private static final int MAX_NUM = 4; //保存抓包数据的最大数量 -- tips:如果按时间来管理最大包数量，可以使用包名上的时间
//    private static final String AUTO_STOP_TIME = "112500"; //格式:HHmmss 已放到strings.xml中
    private String rootPath;
    private String fileDirName;
    private String fileName; //包文件名
    private String screenID = "000000"; //默认ID
    private boolean isChecked = false;
    private boolean firstUpload = false;
    private boolean hasStartTask = false;
    private long startTime; //开始检查CPU内存状况的时间
    private long endTime; //正在检查CPU内存状况的时间
    private static final int CHECK_DURATION = 30; //检查CPU内存状况的持续时间（min）
    private Timer checkTimer;
    //    private RunningInfoHelper helper;

    @Override
    public void onCreate() {
        presenter = new MainActivityPresenter(this);
        CommandsHelper.setOnCaptureDataListener(this);
        ArrayList<File> pcapFiles = presenter.getPcapFiles();
        //处理SD卡中的包的数量
        presenter.handlePcapFils(pcapFiles, MAX_NUM);
        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        helper = RunningInfoHelper.getInstace(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return START_STICKY_COMPATIBILITY;
        }
        screenID = bundle.getString("ID");
        if (BaseApplication.getInstance().getScreenID() == null || !BaseApplication.getInstance().getScreenID().equals(screenID)) {
            BaseApplication.getInstance().setScreenID(screenID);
        }
        String action = bundle.getString("action");
        if (action != null) {
            LogManager.getLogger().e("抓包APP收到的指令: %s", action);
            if (action.equals("start")) {
                if (!isChecked) {
                    //首次接到广播后，查询数据库并进行上传
                    if (checkAndUpload()) {
                        return START_STICKY_COMPATIBILITY;
                    }
                }
                startCapture();
            } else if (action.equals("stop")) {
                CommandsHelper.stopCapture();
            } else if (action.equals("upload")) {
                if (CommandsHelper.isCaptruing) {
                    LogManager.getLogger().e("不能在抓包过程中上传数据包");
                    return START_STICKY_COMPATIBILITY;
                }
                // 上传抓包文件
                presenter.uploadFile(null);
            } else if (action.equals("clear")) {
                if (CommandsHelper.isCaptruing) {
                    LogManager.getLogger().e("不能在抓包过程中清除数据");
                    return START_STICKY_COMPATIBILITY;
                }
                presenter.clearAllPcapFiles();
                PackageDao.clearAll(); // 同时清空数据库
                LogManager.getLogger().e("数据库清空后: %s", PackageDao.queryAll().toString());
            } else if (action.equals("startCheck")) {
                doCheckTask();
            } else if (action.equals("stopCheck")) {
                stopcheck();
            }
        }
        return START_STICKY_COMPATIBILITY;
    }

    /**
     * 停止监控
     */
    private void stopcheck() {
        if (checkTimer != null) {
            checkTimer.cancel();
        }
    }

    /**
     * 监控CPU内存使用状况
     */
    private void doCheckTask() {
        startTime = System.currentTimeMillis();
        endTime = startTime;
        checkTimer = new Timer();
        TimerTask checkTask = new TimerTask() {
            @Override
            public void run() {
                presenter.getCupUsageInfo();
                presenter.getMemoryUsageInfo();
                endTime = System.currentTimeMillis();
                if (endTime - startTime > CHECK_DURATION * 60 * 1000) {
                    checkTimer.cancel();
                    checkTimer = null;
                }
            }
        };
        checkTimer.scheduleAtFixedRate(checkTask, 0L, 3000);
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
                Date autuoUploadDate = sdf2.parse(time + getResources().getString(R.string.auto_stop_time));
                // 如果今天的时间已经过了 首次运行时间就改为明天
                if (System.currentTimeMillis() > autuoUploadDate.getTime()) {
                    autuoUploadDate = new Date(autuoUploadDate.getTime() + period);
                }
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        LogManager.getLogger().e("定时任务已执行");
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

    private boolean checkAndUpload() {
        LogManager.getLogger().e("查询数据库并上传条件: %s %s", isChecked, BaseApplication.getInstance().getScreenID());
        if (!isChecked && BaseApplication.getInstance().getScreenID() != null) {
            // 查询数据库，最新的一条数据的是否已上传？
            isChecked = true;
            List<PackageInfo> packageInfos = PackageDao.queryUploadFlag(false, 1);
            LogManager.getLogger().e("APP打开时查询数据库: %s", packageInfos.toString());
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

    private void startCapture() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            LogManager.getLogger().e("未找到SD卡");
            return;
        }
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

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        String time = sdf.format(date);
        return time;
    }

    @Override
    public void onStartCaptrue() {
        //开始抓包后在数据库中插入一条数据
        PackageDao.insert(new PackageInfo(null, fileName, new Date().getTime(), false));
        LogManager.getLogger().e("数据库中所有数据: %s", PackageDao.queryAll().toString());
        startTimerTask();
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

    @Override
    public void onDestroy() {
        CommandsHelper.stopCapture();
//        unregisterReceiver(actionReceiver);
        presenter.disConnect();
        LogManager.getLogger().e("抓包APP已退出");
    }
}
