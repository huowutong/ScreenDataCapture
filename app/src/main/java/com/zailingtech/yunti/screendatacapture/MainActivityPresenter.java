package com.zailingtech.yunti.screendatacapture;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author LUTAO
 * @date 2017/02/10
 */

public class MainActivityPresenter {

    private MainActivity mainActivity;
    private FTPManager ftpManager;

    public MainActivityPresenter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        ftpManager = new FTPManager(mainActivity);
        ftpManager.setOnUploadListener(mainActivity);
        // 与FTP服务器建立连接
        ftpManager.executeConnectRequest();
    }

    /**
     * 上传文件
     *
     * @param fileName null:上传pcap文件夹中最新的数据包；!null:上传文件名对应的数据包
     */
    public void uploadFile(String fileName) {
        if (BaseApplication.getScreenID() == null) {
            return;
        }
        File pcapFile = null;
        if (fileName != null) {
            String pcapDirName = mainActivity.getResources().getString(R.string.pcap_dir_name);
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
        String pcapDirName = mainActivity.getResources().getString(R.string.pcap_dir_name);
        File pcapDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + pcapDirName);
        if (!pcapDir.exists() && !pcapDir.isDirectory()) {
            pcapDir.mkdir();
        } else {
            File[] files = pcapDir.listFiles();
            ArrayList<File> pcapFiles = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(pcapDirName)) {
                    pcapFiles.add(file);
                }
            }
            Collections.sort(pcapFiles);
            return pcapFiles;
        }
        return null;
    }

    public void handlePcapFils(ArrayList<File> pcapFiles, int maxNum) {
        LogManager.getLogger().e("pcap文件夹中数据包详情: %d %s", pcapFiles.size(), pcapFiles.toString());
        if (pcapFiles == null) {
            return;
        }
        if (pcapFiles.size() > maxNum) {
            for (int i = 0; i < pcapFiles.size() - maxNum; i++) {
                pcapFiles.get(i).delete();
            }
        }
    }

    /**
     * 清空抓包文件
     */
    public void clearAllPcapFiles() {
        String pcapDirName = mainActivity.getResources().getString(R.string.pcap_dir_name);
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
}
