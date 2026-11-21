package com.classmute.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

    TextView statusText;
    Switch enableSwitch;
    View permissionCard;
    LinearLayout classList;
    TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.status_text);
        enableSwitch = (Switch) findViewById(R.id.enable_switch);
        permissionCard = findViewById(R.id.permission_card);
        classList = (LinearLayout) findViewById(R.id.class_list);
        emptyText = (TextView) findViewById(R.id.empty_text);

        // set this before adding the listener otherwise it fires right away
        enableSwitch.setChecked(ClassStore.isEnabled(this));
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ClassStore.setEnabled(MainActivity.this, isChecked);
                Scheduler.refresh(MainActivity.this);
                updateScreen();
            }
        });

        findViewById(R.id.permission_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        });

        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddClassActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // user might of just given us the permission, so check again
        Scheduler.refresh(this);
        updateScreen();
    }

    void updateScreen() {
        statusText.setText(Scheduler.getStatusText(this));

        if (Scheduler.hasDndAccess(this)) {
            permissionCard.setVisibility(View.GONE);
        } else {
            permissionCard.setVisibility(View.VISIBLE);
        }

        ArrayList<ClassItem> classes = ClassStore.load(this);
        classList.removeAllViews();

        if (classes.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }

        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < classes.size(); i++) {
            ClassItem item = classes.get(i);
            View card = inflater.inflate(R.layout.item_class, classList, false);

            ((TextView) card.findViewById(R.id.class_name)).setText(item.name);
            ((TextView) card.findViewById(R.id.class_time)).setText(item.getTimeText());
            ((TextView) card.findViewById(R.id.class_days)).setText(item.getDaysText());

            if (item.isHappeningNow()) {
                card.findViewById(R.id.now_badge).setVisibility(View.VISIBLE);
            }

            final int index = i;
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, AddClassActivity.class);
                    intent.putExtra("index", index);
                    startActivity(intent);
                }
            });

            classList.addView(card);
        }
    }
}
