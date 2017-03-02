package com.zailingtech.yunti.screendatacapture;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

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

    public static Process initSuProcess() throws IOException {
        suProcess = Runtime.getRuntime().exec("su");
        return suProcess;
    }

    public static void startCapture(String fileName) {
        try {
            if (isCaptruing == false) {
                initSuProcess();
                String[] commands = new String[1];
                commands[0] = "tcpdump -vv -s 0 -w " + ROOT_DIR + fileName;
                execCmd(commands, suProcess);
                Log.e("抓包状态", "开始抓包");
                isCaptruing = true;
                suProcess.waitFor();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void stopCapture() {
        if (suProcess != null && isCaptruing == true) {
            suProcess.destroy();
            suProcess = null;
            isCaptruing = false;
            Log.e("抓包状态", "停止抓包");
        }
    }

    public static void execCmd(String[] commands, Process process) {
        try {
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String cmd : commands) {
                if (!TextUtils.isEmpty(cmd)) {
                    os.writeBytes(cmd + "\n");
                }
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
