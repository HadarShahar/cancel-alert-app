package com.myapp.cancelalert;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Calendar;


public class RingingsBoard extends AppCompatActivity {
    private static final String[] RINGINGS = {
            "שיעור 0:    07:45-08:30",
            "שיעור 1:    08:30-09:15",
            "שיעור 2:    09:15-10:00",
            "הפסקה:     10:00-10:15",
            "שיעור 3:    10:15-11:00",
            "שיעור 4:    11:00-11:45",
            "הפסקה:     11:45-12:10",
            "שיעור 5:    12:10-12:55",
            "שיעור 6:    12:55-13:40",
            "הפסקה:     13:40-13:55",
            "שיעור 7:    13:55-14:40",
            "שיעור 8:    14:40-15:25",
            "הפסקה:     15:25-15:35",
            "שיעור 9:    15:35-16:20",
            "שיעור 10:  16:20-17:05",
            "שיעור 11:  17:05-17:50",
            "שיעור 12:  17:50-18:35",
            "שיעור 13:  18:35-19:20"
    };
    private static final int DIVIDER_RINGINGS = 9;

    private static int currentLessonHour = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringings_board);

        currentLessonHour = getCurrentLessonHour();

        TextView currentLessonMarker = (TextView) findViewById(R.id.currentLessonMarker);
        if (currentLessonHour == -1) {
            currentLessonMarker.setVisibility(View.GONE);
        } else {
            currentLessonMarker.setVisibility(View.VISIBLE);
        }


        ListView listView1 = (ListView) findViewById(R.id.listview1);
        // Create an ArrayAdapter from List
        ArrayAdapter<String> arrayAdapter1 = new ArrayAdapter<String>
                (this, R.layout.ringings_listview, Arrays.copyOfRange(RINGINGS, 0, DIVIDER_RINGINGS)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the current item from ListView
                View view = super.getView(position, convertView, parent);

                String ringing = RINGINGS[position];
                if (position == currentLessonHour) {
                    view.setBackgroundColor(getResources().getColor(R.color.currentLessonColor));
                } else if (ringing.contains("הפסקה")) {
                    view.setBackgroundColor(getResources().getColor(R.color.pickedColor));
                }

                return view;
            }
        };
        listView1.setAdapter(arrayAdapter1);


        ListView listView2 = (ListView) findViewById(R.id.listview2);
        // Create an ArrayAdapter from List
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<String>
                (this, R.layout.ringings_listview, Arrays.copyOfRange(RINGINGS, DIVIDER_RINGINGS, RINGINGS.length)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the current item from ListView
                View view = super.getView(position, convertView, parent);

                position += DIVIDER_RINGINGS;

                String ringing = RINGINGS[position];
                if (position == currentLessonHour) {
                    view.setBackgroundColor(getResources().getColor(R.color.currentLessonColor));
                } else if (ringing.contains("הפסקה")) {
                    view.setBackgroundColor(getResources().getColor(R.color.pickedColor));
                }

                return view;
            }
        };
        listView2.setAdapter(arrayAdapter2);


        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.changes_icon:
                        Intent intent = new Intent(RingingsBoard.this, MainActivity.class);
                        // in order to avoid the execution of onCreate in MainActivity
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        break;
                    case R.id.schedule_icon:
                        Intent intent1 = new Intent(RingingsBoard.this, ScheduleTable.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent1);
                        break;
                }
                return true;
            }
        });

    }


    public static int getCurrentLessonHour() {
        Calendar now = Calendar.getInstance();
        int currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

//        let startTimes = ["7:45", "8:30", "9:15", "10:00", "10:15", "11:00", "11:45", "12:10", "12:55", "13:40", "13:55", "14:40", "15:25", "15:35", "16:20", "17:05", "17:50", "18:35", "19:20"];
//        let arrMinTimes = [];
//        for (let i = 0; i < startTimes.length; i++) {
//            let current = startTimes[i].split(":");
//            arrMinTimes.push(parseInt(current[0]) * 60 + parseInt(current[1]));
//        }

        // 7:45, 8:30, 9:15, 10:00, 10:15, 11:00, 11:45, 12:10, 12:55, 13:40, 13:55, 14:40, 15:25, 15:35, 16:20, 17:05, 17:50, 18:35, 19:20
        int[] arrMinTimes = {465, 510, 555, 600, 615, 660, 705, 730, 775, 820, 835, 880, 925, 935, 980, 1025, 1070, 1115, 1160};

        for (int i = 0; i < arrMinTimes.length - 1; i++) {
            if (currentMin >= arrMinTimes[i] && currentMin < arrMinTimes[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onPause() {
        super.onPause();
        // in order to remove the animation
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.bell_icon);
    }

}
