package com.myapp.cancelalert;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class ScheduleTable extends AppCompatActivity implements FetchDataCallbackInterface {

    public static final int NUMBER_OF_DAYS = 6;
    public static final int MAX_HOURS_NUM = 14; // the max number of hours a day
    public static final String SCHEDULE_FILE_NAME = "schedule.txt";

    public static String[] selectedLessons;
    public static String[][] editedScheduleTable = new String[NUMBER_OF_DAYS][MAX_HOURS_NUM];
    public static int currentDayNum;
    public static boolean dataIsReady = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_table);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        Calendar calendar = Calendar.getInstance();
        currentDayNum = calendar.get(Calendar.DAY_OF_WEEK) - 1; // currentDayNum 0 = Sunday
        mViewPager.setCurrentItem(currentDayNum, false);


        loadData();


        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.schedule_icon);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.bell_icon:
                        Intent intent = new Intent(ScheduleTable.this, RingingsBoard.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                        break;
                    case R.id.changes_icon:
                        Intent intent1 = new Intent(ScheduleTable.this, MainActivity.class);
                        // in order to avoid the execution of onCreate in MainActivity
                        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent1);
                        break;
                }
                return true;
            }
        });


    }


    public void loadData() {
        selectedLessons = LessonsSelection.getSelectedLessons(this);

        if (selectedLessons.length <= 1) {
            Toast.makeText(this, "לא נבחרו שיעורים", Toast.LENGTH_SHORT).show();
        }
        String data = FileUtils.readFromFile(SCHEDULE_FILE_NAME, this);
        if (data.equals("")) {

            int classId = MainActivity.ClassId;
            if (classId != -1) {
                String url = RotbergAPI.getScheduleUrl(classId);

                // to get the schedule from the API
                new FetchData(url, this).execute();
            }
        } else {
            // if the data is already saved on te phone
            parseData(data);
        }

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
        bottomNavigationView.setSelectedItemId(R.id.schedule_icon);
    }


    @Override
    public void fetchDataCallback(String result) {
        // result is a json string of all the schedule data
        FileUtils.writeToFile(SCHEDULE_FILE_NAME, result, this);
        parseData(result);
    }

    public void parseData(String strData) {
        // strData is a json string of all the schedule data

        ArrayList<String>[][] scheduleArray = new ArrayList[NUMBER_OF_DAYS][MAX_HOURS_NUM];
        for (int i = 0; i < scheduleArray.length; i++) {
            for (int j = 0; j < scheduleArray[i].length; j++) {
                scheduleArray[i][j] = new ArrayList<String>();
            }
        }

        try {
            JSONObject jsonData = new JSONObject(strData);
            JSONArray scheduleObjectArray = jsonData.getJSONArray("Schedule");

            for (int i = 0; i < scheduleObjectArray.length(); i++) {
                JSONObject hourObject = scheduleObjectArray.getJSONObject(i);
                int dayNum = hourObject.getInt("Day");
                int hourNum = hourObject.getInt("Hour");
                JSONArray lessonsObjectsArray = hourObject.getJSONArray("Lessons");
                for (int j = 0; j < lessonsObjectsArray.length(); j++) {
                    JSONObject lessonObject = lessonsObjectsArray.getJSONObject(j);
                    String lessonStr = lessonObject.getString("Subject") + ", " + lessonObject.getString("Teacher") +
                            " (" + lessonObject.getString("Room") + ")";
                    scheduleArray[dayNum][hourNum].add(lessonStr);
                }
            }

            for (int i = 0; i < scheduleArray.length; i++) {
                String[] dailyLessonsArr = new String[scheduleArray[i].length];
                for (int j = 0; j < dailyLessonsArr.length; j++) {
                    dailyLessonsArr[j] = joinArrayList(filterLessons(scheduleArray[i][j]), "\n", j);
                }
                editedScheduleTable[i] = dailyLessonsArr;
            }

            dataIsReady = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String joinArrayList(ArrayList<String> arrayList, String separator, int hourNum) {
        String[] arr = new String[arrayList.size()];
        arr = arrayList.toArray(arr);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = "שיעור " + String.valueOf(hourNum) + ":   " + arr[i];
        }
        return TextUtils.join(separator, arr);
    }


    public ArrayList<String> filterLessons(ArrayList<String> lessons) {
        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < lessons.size(); i++) {
            String lesson = lessons.get(i);
            for (String selectedLesson : selectedLessons) {
                if (!selectedLesson.equals("") && lesson.contains(selectedLesson)) {
                    ret.add(lesson);
                    break;
                }
            }
        }
        return ret;
    }


    public static ArrayList<String> removeEmptyCells(String[] lessons) {
        // remove the last empty lessons
        // find the last lesson, and return all the lessons until it
        // can't use a simple while loop because there might be a cancelled hour in the middle

        int lastLesson = MAX_HOURS_NUM + 1;
        for (int i = lessons.length - 1; i >= 0; i--) {
            if (!lessons[i].equals("")) {
                lastLesson = i;
                break;
            }
        }
        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < lessons.length; i++) {
            if (i <= lastLesson) {
                ret.add(lessons[i]);
            }
        }
        return ret;
    }






    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_schedule_table, container, false);

            if (dataIsReady) {
                int tabNumber = 0;
                try {
                    tabNumber = getArguments().getInt(ARG_SECTION_NUMBER);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                ListView dailyListview = (ListView) rootView.findViewById(R.id.dailyListview);

                int currentLessonHour = RingingsBoard.getCurrentLessonHour();
                int[] breaksIndices = {3, 6, 9, 12};
                if (currentLessonHour == breaksIndices[0] || currentLessonHour == breaksIndices[1] ||
                        currentLessonHour == breaksIndices[2] || currentLessonHour == breaksIndices[3]) {
                    currentLessonHour = -1;
                } else {
                    for (int i = breaksIndices.length - 1; i >= 0; i--) {
                        if (currentLessonHour > breaksIndices[i]) {
                            currentLessonHour -= (i + 1);
                            break;
                        }
                    }
                }

                final int tabNum = tabNumber;
                final int currentHour = currentLessonHour;
                // Create an ArrayAdapter from List
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                        (container.getContext(), R.layout.schedule_listview, removeEmptyCells(editedScheduleTable[tabNum - 1])) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        // Get the current item from ListView
                        View view = super.getView(position, convertView, parent);

                        if (tabNum - 1 == currentDayNum) {
                            if (position == currentHour) {
                                view.setBackgroundColor(getResources().getColor(R.color.currentLessonColor));
                            }
                        }
                        return view;
                    }
                };
                dailyListview.setAdapter(arrayAdapter);


            }


            return rootView;
        }


    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 6 total pages.
            return 6;
        }
    }
}
