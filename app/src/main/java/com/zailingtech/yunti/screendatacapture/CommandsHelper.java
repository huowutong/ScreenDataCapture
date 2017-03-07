package com.zailingtech.yunti.screendatacapture;

import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * @author LUTAO
 * @date 2017/02/08
 * shell指令执行帮助类
 */

public class CommandsHelper {

    private static final String TAG = "CommandsHelper";
    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static boolean isCaptruing = false;
    private static Process suProcess;
    private static CaptureDataListener mlistener;

    public static Process initSuProcess() throws IOException {
        suProcess = Runtime.getRuntime().exec("su");
        return suProcess;
    }

    public static void setOnCaptureDataListener(CaptureDataListener listener) {
        mlistener = listener;
    }

    /**
     * 开始抓包
     * @param fileDirName 文件在SDcard根目录下的存储路径
     * @throws Exception
     */
    public static void startCapture(String fileDirName) throws Exception {

            if (isCaptruing == false) {
                initSuProcess();
                String[] commands = new String[1];
                commands[0] = "tcpdump -vv -s 0 -w " + ROOT_DIR + fileDirName;
                execCmd(commands, suProcess);
                isCaptruing = true;
                String fileName = fileDirName.substring(fileDirName.lastIndexOf("/") + 1);
                mlistener.onStartCaptrue();
                LogManager.getLogger().e("抓包状态: %s", "开始抓包");
                suProcess.waitFor();
            }

    }

    public static void stopCapture() {
        if (suProcess != null && isCaptruing == true) {
            suProcess.destroy();
            suProcess = null;
            isCaptruing = false;
            mlistener.onStopCaptrue();
            LogManager.getLogger().e("抓包状态: %s", "停止抓包");
        }
    }

    public static void execCmd(String[] commands, Process process) throws IOException {

            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String cmd : commands) {
                if (!TextUtils.isEmpty(cmd)) {
                    os.writeBytes(cmd + "\n");
                }
            }
            os.flush();

    }

    private static void copyStream(InputStream is, OutputStream os) {
        final int BUFFER_SIZE = 1024;
        try {
            byte[] bytes = new byte[BUFFER_SIZE];
            for (; ; ) {
                int count = is.read(bytes, 0, BUFFER_SIZE);
                if (count == -1) {
                    break;
                }

                os.write(bytes, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String parseInputStream(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
