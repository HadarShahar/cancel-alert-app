package com.myapp.cancelalert;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

public class LessonsSelection extends AppCompatActivity {
    public static final String CONFIG_FILE_NAME = "config.txt";
    public static final String SEPARATOR = "#";

    public static ArrayList<String>[] allCategories;
    public static ArrayList<String> selectedLessons = new ArrayList<>();
    public static int index = 0;

    public ImageView right_arrow;
    public ImageView left_arrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lessons_selection);

        index = 0;
        selectedLessons = new ArrayList<String>(Arrays.asList(getSelectedLessons(this)));

        MainActivity.mDatabase.child("classes").child(MainActivity.Class + "_grade")
                .child(MainActivity.ClassNumber).child("allCategories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int size = (int) dataSnapshot.getChildrenCount();
                        allCategories = new ArrayList[size];
                        for (int i = 0; i < size; i++) {
                            allCategories[i] = new ArrayList<String>();
                        }

                        int i = 0;
                        for (DataSnapshot category : dataSnapshot.getChildren()) {
                            for (DataSnapshot item : category.getChildren()) {
                                String subject = item.child("subject").getValue().toString();
                                String teacher = item.child("teacher").getValue().toString();
                                allCategories[i].add(subject + ", " + teacher);
                            }
                            i++;
                        }
                        setListView();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });


        right_arrow = (ImageView) findViewById(R.id.right_arrow);
        right_arrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (index > 0) {
                    left_arrow.setVisibility(View.VISIBLE);
                    index--;
                    setListView();
                }
            }
        });

        left_arrow = (ImageView) findViewById(R.id.left_arrow);
        left_arrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (index < allCategories.length - 1) {
                    right_arrow.setVisibility(View.VISIBLE);
                    index++;
                    setListView();
                }
            }
        });


        Button confirmBtn = (Button) findViewById(R.id.confirmBtn);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LessonsSelection.this, MainActivity.class);

                // in order to avoid the execution of onCreate in MainActivity
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }


    public void setListView() {
        TextView counterTextView = (TextView) findViewById(R.id.counterTextView);
        counterTextView.setText(String.valueOf(index + 1) + "/" + String.valueOf(allCategories.length));

        if (index == 0) {
            right_arrow.setVisibility(View.GONE);
        }
        if (index == allCategories.length - 1) {
            left_arrow.setVisibility(View.GONE);
        }

        ListView listView = (ListView) findViewById(R.id.listview);

        // Create an ArrayAdapter from List
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                (this, R.layout.activity_listview, allCategories[index]) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the current item from ListView
                View view = super.getView(position, convertView, parent);

                String lesson = allCategories[index].get(position);
                if (selectedLessons.contains(lesson)) {
                    view.setBackgroundColor(getResources().getColor(R.color.pickedColor));
                }

                return view;
            }
        };
        listView.setAdapter(arrayAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String lesson = allCategories[index].get(position);
                if (selectedLessons.contains(lesson)) {
                    view.setBackgroundColor(getResources().getColor(R.color.defaultBgColor));
                    selectedLessons.remove(lesson);
                } else {
                    view.setBackgroundColor(getResources().getColor(R.color.pickedColor));
                    selectedLessons.add(lesson);
                }

                String[] selectedLessonsArr = new String[selectedLessons.size()];
                selectedLessonsArr = selectedLessons.toArray(selectedLessonsArr);
                String data = TextUtils.join(SEPARATOR, selectedLessonsArr);
                FileUtils.writeToFile(CONFIG_FILE_NAME, data, LessonsSelection.this);
            }
        });
    }

    public static String[] getSelectedLessons(Context context) {
        String data = FileUtils.readFromFile(CONFIG_FILE_NAME, context);
        return data.split(SEPARATOR);
    }

    @Override
    protected void onResume() {
        super.onResume();
        index = 0;
    }
}
