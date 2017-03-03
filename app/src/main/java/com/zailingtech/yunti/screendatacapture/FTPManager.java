package com.zailingtech.yunti.screendatacapture;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

/**
 * @author LUTAO
 * @date 2017/02/28
 */

public class FTPManager {

    private static final String TAG = "FTPManager";
    private final ExecutorService mThreadPool;
    private final FTPClient mFTPClient;
    private final CmdFactory mCmdFactory;
    private final Context context;
    private static final String hostName = "120.26.47.203";
    private static final int serverPort = 21;
    private static final String userName = "chenyu";
    private static final String password = "chenyu123";
    private boolean mDameonRunning = true;
    private static final int MAX_THREAD_NUMBER = 5;
    private static final int MAX_DAMEON_TIME_WAIT = 60 * 1000; // 心跳间隔 millisecond

    private static final int MENU_OPTIONS_BASE = 0;
    private static final int MSG_CMD_CONNECT_OK = MENU_OPTIONS_BASE + 1;
    private static final int MSG_CMD_CONNECT_FAILED = MENU_OPTIONS_BASE + 2;
    private static final int MSG_CMD_LIST_OK = MENU_OPTIONS_BASE + 3;
    private static final int MSG_CMD_LIST_FAILED = MENU_OPTIONS_BASE + 4;
    private static final int MSG_CMD_CWD_OK = MENU_OPTIONS_BASE + 5;
    private static final int MSG_CMD_CWD_FAILED = MENU_OPTIONS_BASE + 6;
    private static final int MSG_FILENAME_REPETITION = MENU_OPTIONS_BASE + 7;

    private String mCurrentPWD; // 当前远程目录
    private Thread mDameonThread;
    private Object mLock = new Object();
    private List<FTPFile> mFileList = new ArrayList<FTPFile>();

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            LogManager.getLogger().e("mHandler --->" + msg.what);
            switch (msg.what) {
                case MSG_CMD_CONNECT_OK:
                    toast("FTP服务器连接成功");
                    if (mDameonThread == null) {
                        //启动守护进程。
                        mDameonThread = new Thread(new DameonFtpConnector());
                        mDameonThread.setDaemon(true);
                        mDameonThread.start();
                    }
                    executeLISTRequest();
                    break;
                case MSG_CMD_CONNECT_FAILED:
                    toast("FTP服务器连接失败，正在重新连接");
                    executeConnectRequest();
                    break;
                case MSG_CMD_LIST_OK:
                    toast("请求数据成功。");
                    break;
                case MSG_CMD_LIST_FAILED:
                    toast("请求数据失败。");
                    break;
                case MSG_CMD_CWD_OK:
                    toast("请求数据成功。");
//                    executeLISTRequest();
                    break;
                case MSG_CMD_CWD_FAILED:
                    toast("请求数据失败。");
                    break;
                case MSG_FILENAME_REPETITION:
                    toast("文件重复，已取消上传。");
                    break;
                default:
                    break;
            }
        }
    };

    public FTPManager(Context context) {
        this.context = context;
        mCmdFactory = new CmdFactory();
        mFTPClient = new FTPClient();
        mThreadPool = Executors.newFixedThreadPool(MAX_THREAD_NUMBER);
    }

    public class DameonFtpConnector implements Runnable {

        @Override
        public void run() {
            LogManager.getLogger().e("DameonFtpConnector ### run");
            while (mDameonRunning) {
                if (mFTPClient != null && !mFTPClient.isConnected()) {
                    try {
                        mFTPClient.connect(hostName, serverPort);
                        mFTPClient.login(userName, password);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                try {
                    if (mFTPClient.isConnected()) {
                        mFTPClient.noop();
                    }
                    Thread.sleep(MAX_DAMEON_TIME_WAIT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (FTPException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FTPIllegalReplyException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void executeConnectRequest() {
        mThreadPool.execute(mCmdFactory.createCmdConnect());
    }

    private void executeDisConnectRequest() {
        mThreadPool.execute(mCmdFactory.createCmdDisConnect());
    }

    private void executePWDRequest() {
        mThreadPool.execute(mCmdFactory.createCmdPWD());
    }

    private void executeLISTRequest() {
        mThreadPool.execute(mCmdFactory.createCmdLIST());
    }

    private void executeCWDRequest(String path) {
        mThreadPool.execute(mCmdFactory.createCmdCWD(path));
    }

    public abstract class FtpCmd implements Runnable {
        public abstract void run();
    }

    /**
     * 建立FTP连接
     */
    public class CmdConnect extends FtpCmd {
        @Override
        public void run() {
            boolean errorAndRetry = false;  //根据不同的异常类型，是否重新捕获
            try {
                String[] welcome = mFTPClient.connect(hostName, serverPort);
                if (welcome != null) {
                    for (String value : welcome) {
                        LogManager.getLogger().e("connect " + value);
                    }
                }
                mFTPClient.login(userName, password);
                mHandler.sendEmptyMessage(MSG_CMD_CONNECT_OK);
            } catch (IllegalStateException illegalEx) {
                illegalEx.printStackTrace();
                errorAndRetry = true;
            } catch (IOException ex) {
                ex.printStackTrace();
                errorAndRetry = true;
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
                errorAndRetry = true;
            }
            if (errorAndRetry && mDameonRunning) {
                mHandler.sendEmptyMessageDelayed(MSG_CMD_CONNECT_FAILED, 2000);
            }
        }
    }

    /**
     * 断开FTP连接
     */
    public class CmdDisConnect extends FtpCmd {

        @Override
        public void run() {
            if (mFTPClient != null) {
                try {
                    mFTPClient.disconnect(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * 当前上传文件所在FPT服务器的根目录
     */
    public class CmdPWD extends FtpCmd {

        @Override
        public void run() {

            try {
                String pwd = mFTPClient.currentDirectory();
                LogManager.getLogger().e("pwd --- > " + pwd);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 当前远程目录下的所有文件
     */
    public class CmdLIST extends FtpCmd {

        @Override
        public void run() {

            try {
                mCurrentPWD = mFTPClient.currentDirectory();
                FTPFile[] ftpFiles = mFTPClient.list();
                LogManager.getLogger().e(" Request Size  : " + ftpFiles.length);
                synchronized (mLock) {
                    mFileList.clear();
                    mFileList.addAll(Arrays.asList(ftpFiles));
                }
                mHandler.sendEmptyMessage(MSG_CMD_LIST_OK);

            } catch (Exception ex) {
                mHandler.sendEmptyMessage(MSG_CMD_LIST_FAILED);
                ex.printStackTrace();
            }
        }
    }

    /**
     * 改变远程目录
     */
    public class CmdCWD extends FtpCmd {

        String realivePath;

        public CmdCWD(String path) {
            realivePath = path;
        }

        @Override
        public void run() {
            try {
                mFTPClient.changeDirectory(realivePath);
                mHandler.sendEmptyMessage(MSG_CMD_CWD_OK);
            } catch (Exception ex) {
                mHandler.sendEmptyMessage(MSG_CMD_CWD_FAILED);
                ex.printStackTrace();
            }
        }
    }

    /**
     * 上传文件
     */
    class CmdUpload extends AsyncTask<String, Integer, Boolean> {

        String path;

        public CmdUpload() { }

        @Override
        protected Boolean doInBackground(String... params) {
            path = params[0];
            try {
                String fileName = path.substring(path.lastIndexOf("/") + 1);
                LogManager.getLogger().e("上传文件名: %s", fileName);
                executeLISTRequest();
                for (FTPFile ftpFile : mFileList) {
                    LogManager.getLogger().e("FTP服务器文件: %s", ftpFile.getName());
                    if (ftpFile.getName().equals(fileName)) {
                        mHandler.sendEmptyMessage(MSG_FILENAME_REPETITION);
                        return false;
                    }
                }
                File file = new File(path);
                mFTPClient.upload(file, new DownloadFTPDataTransferListener(
                        file.length()));
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }

            return true;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Boolean result) {
            LogManager.getLogger().e("上传结果: %s", path + "  结果：" +result);
            toast(result ? path + "上传成功" : "上传失败");
        }
    }

    private class DownloadFTPDataTransferListener implements
            FTPDataTransferListener {

        private int totolTransferred = 0;
        private long fileSize = -1;

        public DownloadFTPDataTransferListener(long fileSize) {
            if (fileSize <= 0) {
                throw new RuntimeException(
                        "the size of file muset be larger than zero.");
            }
            this.fileSize = fileSize;
        }

        @Override
        public void aborted() {
            LogManager.getLogger().e("FTPDataTransferListener : aborted");
        }

        @Override
        public void completed() {
            LogManager.getLogger().e("FTPDataTransferListener : completed");
        }

        @Override
        public void failed() {
            LogManager.getLogger().e("FTPDataTransferListener : failed");
        }

        @Override
        public void started() {
            LogManager.getLogger().e("FTPDataTransferListener : started");
        }

        @Override
        public void transferred(int length) {
            totolTransferred += length;
            float percent = (float) totolTransferred / this.fileSize;
            LogManager.getLogger().e("FTPDataTransferListener : transferred # percent ： " + percent);
        }
    }

    public class CmdFactory {
        public FtpCmd createCmdConnect() {
            return new CmdConnect();
        }

        public FtpCmd createCmdDisConnect() {
            return new CmdDisConnect();
        }

        public FtpCmd createCmdPWD() {
            return new CmdPWD();
        }

        public FtpCmd createCmdLIST() {
            return new CmdLIST();
        }

        public FtpCmd createCmdCWD(String path) {
            return new CmdCWD(path);
        }
    }

    public void disConnect() {
        mDameonRunning = false;
        Thread thread = new Thread(mCmdFactory.createCmdDisConnect());
        thread.start();
        //等待连接中断
        try {
            thread.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThreadPool.shutdownNow();
    }

    private void toast(String hint) {
        Toast.makeText(context, hint, Toast.LENGTH_SHORT).show();
    }
}
