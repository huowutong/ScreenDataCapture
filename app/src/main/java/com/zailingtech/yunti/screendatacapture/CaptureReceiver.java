package com.zailingtech.yunti.screendatacapture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Description: 抓包静态广播接收 <br>
 * Author: LUTAO <br>
 * Date: 2017/03/17 <br>
 */

public class CaptureReceiver extends BroadcastReceiver {

    private boolean isChecked = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        Intent serviceIntent = new Intent(context, CaptureService.class);
        serviceIntent.putExtras(bundle);
        context.startService(serviceIntent);
    }
}
