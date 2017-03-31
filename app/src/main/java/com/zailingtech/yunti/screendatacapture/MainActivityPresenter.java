package com.zailingtech.yunti.screendatacapture;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

import com.zailingtech.yunti.screendatacapture.model.PackageInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author LUTAO
 * @date 2017/02/10
 */

public class MainActivityPresenter {

    private Context context;
    private FTPManager ftpManager;
    private RunningInfoHelper helper;

    public MainActivityPresenter(Context context) {
        this.context = context;
        ftpManager = new FTPManager(context);
        if (context instanceof MainActivity) {
            ftpManager.setOnUploadListener((MainActivity)context);
        } else if (context instanceof CaptureService) {
            ftpManager.setOnUploadListener((CaptureService)context);
        }
        helper = RunningInfoHelper.getInstace(context);
        // 与FTP服务器建立连接
        ftpManager.executeConnectRequest();
        SystemClock.sleep(1000); //休眠1s等待子线程中的登陆完成
    }

    /**
     * 上传文件
     *
     * @param fileName null:上传pcap文件夹中最新的数据包；!null:上传文件名对应的数据包
     */
    public void uploadFile(String fileName) {
        if (BaseApplication.getInstance().getScreenID() == null) {
            return;
        }
        File pcapFile = null;
        if (fileName != null) {
            String pcapDirName = context.getResources().getString(R.string.pcap_dir_name);
            String pcapDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + pcapDirName + File.separator + fileName;
            //上传文件到FTP
            FTPManager.CmdUpload cmdUpload = ftpManager.new CmdUpload();
            cmdUpload.execute(pcapDir);
        } else {
            ArrayList<File> pcapFiles = getPcapFiles();
            if (pcapFiles == null || pcapFiles.size() == 0) {
                return;
            }
            pcapFile = pcapFiles.get(pcapFiles.size() - 1);
            //上传文件到FTP
            FTPManager.CmdUpload cmdUpload = ftpManager.new CmdUpload();
            cmdUpload.execute(pcapFile.getAbsolutePath());
        }
    }

    public ArrayList<File> getPcapFiles() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }
        String pcapDirName = context.getResources().getString(R.string.pcap_dir_name);
        File pcapDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), pcapDirName);
        if (!pcapDir.exists() && !pcapDir.mkdir()) {
            return null;
        }
        File[] files = pcapDir.listFiles();
        ArrayList<File> pcapFiles = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith("pcap")) {
                pcapFiles.add(file);
            }
        }
        Collections.sort(pcapFiles);
        return pcapFiles;
    }

    public void handlePcapFils(ArrayList<File> pcapFiles, int maxNum) {
        LogManager.getLogger().e("pcap文件夹中数据包详情: %d %s", pcapFiles.size(), pcapFiles.toString());
        if (pcapFiles == null) {
            return;
        }
        if (pcapFiles.size() > maxNum) {
            for (int i = 0; i < pcapFiles.size() - maxNum; i++) {
                pcapFiles.get(i).delete();
                List<PackageInfo> packageInfos = PackageDao.queryFileName(pcapFiles.get(i).getName());
                if (packageInfos == null || packageInfos.size() == 0) {
                    continue;
                }
                PackageDao.delete(packageInfos.get(0).getId()); // 删除本地数据的同时删除数据库中对应的记录
            }
        }
    }

    /**
     * 清空抓包文件
     */
    public void clearAllPcapFiles() {
        String pcapDirName = context.getResources().getString(R.string.pcap_dir_name);
        File pcapDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + pcapDirName);
        if (pcapDir.exists() && pcapDir.isDirectory()) {
            File[] pcaps = pcapDir.listFiles();
            if (pcaps.length > 0) {
                for (File pcap : pcaps) {
                    pcap.delete();
                }
            }
        }
        LogManager.getLogger().e("清空文件后的pcap文件夹大小: %d", pcapDir.listFiles().length);
    }

    public void disConnect() {
        ftpManager.disConnect();
    }

    public float getCupUsageInfo() {
        long totalMemory = helper.getTotalMemory();
        long freeMemorySize = helper.getFreeMemorySize();
        float memoryUsage = (totalMemory - freeMemorySize) / (float) totalMemory * 100;
        LogManager.getLogger().e("内存使用率：%.1f%%", memoryUsage);
        return memoryUsage;

    }

    public float getMemoryUsageInfo() {
        float cpuUsage = helper.getCpuUsage();
        LogManager.getLogger().e("CPU使用率：%.1f%%", cpuUsage * 100);
        return cpuUsage;
    }
}
