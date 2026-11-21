package com.classmute.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ClassAlarmReceiver extends BroadcastReceiver {

    // runs when our alarm goes off and also when the phone restarts,
    // becuase android forgets all alarms on reboot
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.refresh(context);
    }
}
