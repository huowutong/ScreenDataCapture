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
        // 与FTP服务器建立连接
        ftpManager.executeConnectRequest();
    }

    public void uploadFile() {
        ArrayList<File> pcapFiles = getPcapFiles();
        if (pcapFiles == null || pcapFiles.size() == 0) {
            return;
        }
        File pcapFile = pcapFiles.get(pcapFiles.size() - 1);
        //上传文件到FTP
        FTPManager.CmdUpload cmdUpload = ftpManager.new CmdUpload();
        cmdUpload.execute(pcapFile.getAbsolutePath());
    }

    public ArrayList<File> getPcapFiles() {
        File pcapDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "pcap");
        if (!pcapDir.exists() && !pcapDir.isDirectory()) {
            pcapDir.mkdir();
        } else {
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

    public void disConnect() {
        ftpManager.disConnect();
    }
}
