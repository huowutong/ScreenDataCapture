package com.zailingtech.yunti.screendatacapture;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;

import com.zailingtech.yunti.screendatacapture.model.CPUTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Description: 查看设备运行时的内存、CPU使用状况 <br>
 * Author: LUTAO <br>
 * Date: 2017/03/27 <br>
 */

public class RunningInfoHelper {

    private static RunningInfoHelper mInstance;
    private Context context;
    private static final String procPath = File.separator + "proc" + File.separator + "stat";
    private long startTime;
    private long endTime;

    public RunningInfoHelper(final Context context) {
        this.context = context;
    }

    public static final RunningInfoHelper getInstace(final Context context) {
        if (mInstance == null) {
            mInstance = new RunningInfoHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * 获取设备可用内存
     */
    public long getFreeMemorySize() {
        ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(outInfo);
        long avaliMem = outInfo.availMem;
        return avaliMem / 1024;
    }

    /**
     * 获取设备总内存
     */
    public long getTotalMemory() {
        String memInfoPath = "/proc/meminfo";
        String readTemp = "";
        String memTotal = "";
        long memory = 0;
        try {
            FileReader fr = new FileReader(memInfoPath);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            while ((readTemp = localBufferedReader.readLine()) != null) {
                if (readTemp.contains("MemTotal")) {
                    String[] total = readTemp.split(":");
                    memTotal = total[1].trim();
                }
            }
            localBufferedReader.close();
            String[] memKb = memTotal.split(" ");
            memTotal = memKb[0].trim();
            memory = Long.parseLong(memTotal);
        } catch (IOException e) {
            LogManager.getLogger().e("IOException: " + e.getMessage());
        }
        return memory;
    }

    /**
     * 获得CPU时间：空闲时间和总时间
     */
    private void getcpuTime(CPUTime t) {
        BufferedReader fr = null;
        try {
            fr = new BufferedReader(new FileReader(procPath));

            String oneLine = null;
            while ((oneLine = fr.readLine()) != null) {
                if (oneLine.startsWith("cpu")) {
                    String[] vals = oneLine.substring(4).trim().split(" ");
                    if (vals.length != 10) {
                        System.err.println("read an error line string!");
                    } else {
                        t.setTotalTime(Long.parseLong(vals[1]) + Long.parseLong(vals[2]) + Long.parseLong(vals[3])
                                + Long.parseLong(vals[4]) + Long.parseLong(vals[5]) + Long.parseLong(vals[6])
                                + Long.parseLong(vals[7]) + Long.parseLong(vals[8]) + Long.parseLong(vals[9]));
                        t.setIdleTime(Long.parseLong(vals[4]));
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 计算CPU使用率
     */
    public float getCpuUsage() {
        CPUTime startTime = new CPUTime();
        CPUTime endTime = new CPUTime();

        getcpuTime(startTime);
        SystemClock.sleep(1000);
        getcpuTime(endTime);

        float cpuUsage = 0;
        long totalTime = endTime.getTotalTime() - startTime.getTotalTime();
        if (totalTime == 0) {
            cpuUsage = 0;
        } else {
            cpuUsage = (endTime.getIdleTime() - startTime.getIdleTime()) / (float) totalTime;
        }
        return cpuUsage;
    }
}
