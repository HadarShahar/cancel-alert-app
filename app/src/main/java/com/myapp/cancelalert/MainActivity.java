package com.myapp.cancelalert;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static final int CLASSES_NUM = 10;
    public static final String[] ENGLISH_CLASSES = {"10th", "11th", "12th"};
    public static final String[] HEBREW_CLASSES = {"י'", "י\"א", "י\"ב"};
    public static final String USERS_DATABASE = "users2";

    public static DatabaseReference mDatabase;
    public static String Class = "";
    public static String ClassNumber = "";
    public static int ClassId = -1;
    public static String phoneId = "";

    public static boolean finishLoading = false;
    public static String allChangesData = "";
    public static String[] selectedLessons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        MobileAds.initialize(this, "ca-app-pub-7937869727957992~5508375844");
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        TextView changesView = (TextView) findViewById(R.id.changesView);
        changesView.setMovementMethod(new ScrollingMovementMethod());


        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        phoneId = FirebaseInstanceId.getInstance().getToken();
                        mDatabase.child(USERS_DATABASE).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.hasChild(phoneId)) {
                                    Class = (String) dataSnapshot.child(phoneId).child("Class").getValue();
                                    ClassNumber = (String) dataSnapshot.child(phoneId).child("ClassNumber").getValue();
                                } else {
                                    mDatabase.child(USERS_DATABASE).child(phoneId).child("Class").setValue("");
                                    mDatabase.child(USERS_DATABASE).child(phoneId).child("ClassNumber").setValue("");
                                }

                                finishLoading = true;
                                updateSpinners();
                                updateViewBtn();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });

                    }
                });



        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateClass();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        };

        Spinner classNumberSpinner = (Spinner) findViewById(R.id.classNumberSpinner);
        classNumberSpinner.setOnItemSelectedListener(itemSelectedListener);

        Spinner classSpinner = (Spinner) findViewById(R.id.classSpinner);
        classSpinner.setOnItemSelectedListener(itemSelectedListener);




        Button viewWebsite = (Button) findViewById(R.id.viewWebsite);
        viewWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "http://rotberg.iscool.co.il/%D7%93%D7%A3%D7%91%D7%99%D7%AA/tabid/4867/language/he-IL/Default.aspx";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });


        Button lessonsSelectionBtn = (Button) findViewById(R.id.lessonsSelectionBtn);
        lessonsSelectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finishLoading && !Class.equals("")) {
                    Intent myIntent = new Intent(MainActivity.this, LessonsSelection.class);
                    startActivity(myIntent);
                }
            }
        });


        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.bell_icon:
                        Intent intent = new Intent(MainActivity.this, RingingsBoard.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                        break;
                    case R.id.changes_icon:
                        onclickShowChanges();
                        break;
                    case R.id.schedule_icon:
                        Intent intent1 = new Intent(MainActivity.this, ScheduleTable.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent1);
                        break;
                }
                return true;
            }
        });

    }


    public static String getHebrewClass() {
        for (int i = 0; i < ENGLISH_CLASSES.length; i++) {
            if (Class.equals(ENGLISH_CLASSES[i])) {
                return HEBREW_CLASSES[i];
            }
        }
        return "";
    }


    public void updateClass() {
        if (finishLoading) {

            Spinner classSpinner = findViewById(R.id.classSpinner);
            // String selectedClass = (String) classSpinner.getSelectedItem();
            int selectedClassPos = classSpinner.getSelectedItemPosition();
            String englishClass = ENGLISH_CLASSES[selectedClassPos];

            Spinner classNumberSpinner = findViewById(R.id.classNumberSpinner);
            String selectedClassNumber = (String) classNumberSpinner.getSelectedItem();


            // exit if it's the same class
            if (englishClass.equals(Class) && selectedClassNumber.equals(ClassNumber)) {
                return;
            }
            FileUtils.writeToFile(LessonsSelection.CONFIG_FILE_NAME, "", this);


            mDatabase.child(USERS_DATABASE).child(phoneId).child("Class").setValue(englishClass);
            mDatabase.child(USERS_DATABASE).child(phoneId).child("ClassNumber").setValue(selectedClassNumber);

            FirebaseMessaging.getInstance().unsubscribeFromTopic("class_" + Class + "_" + ClassNumber);
            FirebaseMessaging.getInstance().subscribeToTopic("class_" + englishClass + "_" + selectedClassNumber);
            Class = englishClass;
            ClassNumber = selectedClassNumber;

            Toast.makeText(MainActivity.this, "כיתתך עודכנה לכיתה " + getHebrewClass() + ClassNumber, Toast.LENGTH_SHORT).show();

            // clear the schedule data
            FileUtils.writeToFile(ScheduleTable.SCHEDULE_FILE_NAME, "", this);

            setSelectedLessonsCounter();
            updateViewBtn();

        }
    }


    public void updateViewBtn() {
        if (!Class.equals("") && !ClassNumber.equals("")) {

            mDatabase.child("classes").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                    // if the class doesn't exist
                    //------------------------------------------------------------------------------
                    if (!dataSnapshot.child(Class + "_grade").hasChild(ClassNumber)) {
                        TextView contentTitle = (TextView) findViewById(R.id.contentTitle);
                        contentTitle.setText("אין כיתה כזאת...");
                        TextView changesView = (TextView) findViewById(R.id.changesView);
                        changesView.setText("");
                        ClassId = -1;
                        return;
                    }
                    //------------------------------------------------------------------------------

                    ClassId = Integer.parseInt(dataSnapshot.child(Class + "_grade").child(ClassNumber).child("classId").getValue().toString());
                    int currentNum = Integer.parseInt(dataSnapshot.child(Class + "_grade").child(ClassNumber).child("currentNum").getValue().toString());
                    // currentLessonHour = Integer.parseInt(dataSnapshot.child("currentLessonHour").getValue().toString());

                    allChangesData = "";
                    for (DataSnapshot postSnapshot : dataSnapshot.child(Class + "_grade").child(ClassNumber).child("changes").getChildren()) {
                        String date = postSnapshot.child("date").getValue().toString();
                        String hour = postSnapshot.child("hour").getValue().toString();
                        String subject = postSnapshot.child("subject").getValue().toString();
                        String teacher = postSnapshot.child("teacher").getValue().toString();

                        date = date.replace("/Date(", "");
                        date = date.replace(")/", "");
                        Date newDate = new Date(Long.parseLong(date));
                        DateFormat formatter = new SimpleDateFormat("dd/MM");
                        String finalDate = formatter.format(newDate);


                        String changeData = "ביטול " + subject + ", " + teacher + ", " + "שיעור " + hour + " (" + finalDate + ")\n";
                        boolean broken = false;
                        for (String selectedLesson : selectedLessons) {
                            if (!selectedLesson.equals("") && changeData.contains(selectedLesson)) {
                                allChangesData += changeData;
                                broken = true;
                                break;
                            }
                        }
                        if (!broken) {
                            currentNum--;
                        }
                    }


                    Button viewWebsite = (Button) findViewById(R.id.viewWebsite);
                    if (currentNum == 0) {
                        viewWebsite.setBackgroundResource(R.drawable.rounded_shape_blue);
                        viewWebsite.setText("למעבר לאתר רוטברג");
                    } else {
                        viewWebsite.setBackgroundResource(R.drawable.rounded_shape_red);
                        viewWebsite.setText("למעבר לאתר רוטברג" + "\n(יש לך " + String.valueOf(currentNum) + " שיעורים מבוטלים)");
                    }

                    onclickShowChanges();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }


    public void onclickShowChanges() {
        if (!Class.equals("") && !ClassNumber.equals("")) {

            TextView changesView = (TextView) findViewById(R.id.changesView);
            TextView contentTitle = (TextView) findViewById(R.id.contentTitle);

            SpannableString content = new SpannableString("לוח ביטולים - " + getHebrewClass() + ClassNumber + ":");
            content.setSpan(new UnderlineSpan(), 0, content.length() - 1, 0);
            contentTitle.setText(content);
            changesView.setText(allChangesData);
            if (allChangesData.equals("")) {
                changesView.setText("אין ביטולים רלוונטיים עבורך");
            }
        }
    }


    public void setSelectedLessonsCounter() {
        selectedLessons = LessonsSelection.getSelectedLessons(this);
        TextView selectedLessonsCounter = (TextView) findViewById(R.id.selectedLessonsCounter);
        if (selectedLessons.length <= 1) {
            selectedLessonsCounter.setTextColor(getResources().getColor(R.color.darkRed));
            selectedLessonsCounter.setText("לא נבחרו שיעורים");
        } else {
            selectedLessonsCounter.setTextColor(getResources().getColor(R.color.black));
            selectedLessonsCounter.setText("נבחרו " + String.valueOf(selectedLessons.length - 1) + " שיעורים");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setSelectedLessonsCounter();
        updateViewBtn();
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.changes_icon);
    }


    public void updateSpinners() {
        Spinner classNumberSpinner = findViewById(R.id.classNumberSpinner);
        String[] classNumbers = new String[CLASSES_NUM];
        for (int i = 0; i < classNumbers.length; i++) {
            classNumbers[i] = String.valueOf(i+1);
        }

        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classNumbers);
        classNumberSpinner.setAdapter(adapter1);
        int position1 = 0;
        for (int i = 1; i < classNumbers.length; i++) {
            if (ClassNumber.equals(classNumbers[i])) {
                position1 = i;
            }
        }
        classNumberSpinner.setSelection(position1);


        Spinner classSpinner = findViewById(R.id.classSpinner);
        String hebrewClass = getHebrewClass();
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, HEBREW_CLASSES);
        classSpinner.setAdapter(adapter2);
        int position2 = 0;
        for (int i = 1; i < HEBREW_CLASSES.length; i++) {
            if (hebrewClass.equals(HEBREW_CLASSES[i])) {
                position2 = i;
            }
        }
        classSpinner.setSelection(position2);

    }


}
