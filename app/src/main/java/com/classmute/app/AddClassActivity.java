package com.classmute.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;

public class AddClassActivity extends Activity {

    EditText nameInput;
    TextView[] dayViews = new TextView[7];
    TextView startText;
    TextView endText;

    int startMinutes = 9 * 60;
    int endMinutes = 10 * 60;
    // -1 means we're adding a new class, anything else is the class being edited
    int editIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);

        nameInput = (EditText) findViewById(R.id.name_input);
        startText = (TextView) findViewById(R.id.start_text);
        endText = (TextView) findViewById(R.id.end_text);
        TextView titleText = (TextView) findViewById(R.id.title_text);
        TextView deleteButton = (TextView) findViewById(R.id.delete_button);

        int[] dayIds = {R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6};
        for (int i = 0; i < 7; i++) {
            dayViews[i] = (TextView) findViewById(dayIds[i]);
            dayViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setSelected(!v.isSelected());
                }
            });
        }

        editIndex = getIntent().getIntExtra("index", -1);
        if (editIndex != -1) {
            ClassItem item = ClassStore.load(this).get(editIndex);
            nameInput.setText(item.name);
            startMinutes = item.startMinutes;
            endMinutes = item.endMinutes;
            for (int i = 0; i < 7; i++) {
                dayViews[i].setSelected(item.days[i]);
            }
            titleText.setText("Edit class");
            deleteButton.setVisibility(View.VISIBLE);
        }
        updateTimes();

        findViewById(R.id.start_box).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime(true);
            }
        });

        findViewById(R.id.end_box).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime(false);
            }
        });

        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveClass();
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    void pickTime(final boolean isStart) {
        int current = endMinutes;
        if (isStart) {
            current = startMinutes;
        }

        TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hour, int minute) {
                if (isStart) {
                    startMinutes = hour * 60 + minute;
                    // move the end time along with it so it doesnt end before it starts
                    if (endMinutes <= startMinutes) {
                        endMinutes = Math.min(startMinutes + 60, 23 * 60 + 59);
                    }
                } else {
                    endMinutes = hour * 60 + minute;
                }
                updateTimes();
            }
        }, current / 60, current % 60, false);
        dialog.show();
    }

    void updateTimes() {
        startText.setText(ClassItem.formatTime(startMinutes));
        endText.setText(ClassItem.formatTime(endMinutes));
    }

    void saveClass() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            showMessage("Give the class a name first");
            return;
        }

        ClassItem item = new ClassItem();
        item.name = name;
        item.startMinutes = startMinutes;
        item.endMinutes = endMinutes;

        boolean anyDay = false;
        for (int i = 0; i < 7; i++) {
            item.days[i] = dayViews[i].isSelected();
            if (item.days[i]) {
                anyDay = true;
            }
        }
        if (!anyDay) {
            showMessage("Pick at least one day");
            return;
        }
        if (endMinutes <= startMinutes) {
            showMessage("The class has to end after it starts");
            return;
        }

        ArrayList<ClassItem> list = ClassStore.load(this);
        if (editIndex == -1) {
            list.add(item);
        } else {
            list.set(editIndex, item);
        }
        ClassStore.save(this, list);
        Scheduler.refresh(this);
        finish();
    }

    void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete this class?")
                .setMessage("It'll be removed from your timetable.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<ClassItem> list = ClassStore.load(AddClassActivity.this);
                        list.remove(editIndex);
                        ClassStore.save(AddClassActivity.this, list);
                        Scheduler.refresh(AddClassActivity.this);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void showMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
