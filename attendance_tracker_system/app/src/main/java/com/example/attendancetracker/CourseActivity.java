package com.example.attendancetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

/**     Semester: Fall 2022
 *  CourseActivity is the screen that is displayed when a
 *  semester is selected on MainActivity.
 *
 *  Currently this screen is for adding a course after a semester
 *  has been created and a course was left out during semester creation.
 *  Navigation to the course's screen is possible from here as well.
 *
 *  This could also be considered an artefact of the previous team, as
 *  courses were only displayed as a separate screen from semesters.
 *  Our team combined semesters and courses into one screen for better
 *  user experience and human-computer interaction improvements.
 */
public class CourseActivity extends AppCompatActivity {
    private static final String TAG = "COURSE_SCREEN";

    private ArrayList<String> courseList = new ArrayList<>();
    private DBHandler db;
    private AdapterCourseRecycler adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);
        // fab to add new course
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addSectionBtn);
        db = new DBHandler(this);

        // get intent results from MainActivity
        String semesterName = getIntent().getStringExtra("semester");
        String userEmail = getIntent().getStringExtra("userEmail");

        // debugging output
        Log.d(TAG, semesterName);
        Log.d(TAG, userEmail);

        initRecyclerView(userEmail, semesterName);

        courseList = db.getCourses(semesterName, userEmail);
        if (courseList == null) {
            courseList = new ArrayList<>();
        }

        fab.setOnClickListener(view -> {
            addCourse(userEmail, semesterName);
            adapter.notifyDataSetChanged();
        });

        setTitle(semesterName);
    }

    private void addCourse(String userEmail, String semesterName) {
        Dialog addCourseDialog = new Dialog(this);
        LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.dialog_course_add, null);
        addCourseDialog.setContentView(layout);
        addCourseDialog.show();

        EditText addCourseEditText = (EditText) addCourseDialog.findViewById(R.id.courseAddName);
        Button acceptBtn = (Button) addCourseDialog.findViewById(R.id.courseAddAcceptBtn);
        Button cancelBtn = (Button) addCourseDialog.findViewById(R.id.courseAddCancelBtn);
        acceptBtn.setOnClickListener(view -> {
            String newCourseName = addCourseEditText.getText().toString().trim();
            if (!newCourseName.isEmpty()) {
                try {
                    courseList.add(newCourseName);
                    boolean result = db.addCourse(newCourseName, semesterName, userEmail);
                    if (!result) {
                        throw new SQLiteConstraintException();
                    }
                    addCourseDialog.dismiss();
                    initRecyclerView(userEmail, semesterName);
                    Toast.makeText(CourseActivity.this,"Course has been added!", Toast.LENGTH_SHORT).show();
                } catch (SQLiteConstraintException e) {
                    Toast.makeText(CourseActivity.this, "This course already exists", Toast.LENGTH_LONG).show();
                }

            }else{
                Toast.makeText(CourseActivity.this,"New course name cannot be empty.", Toast.LENGTH_LONG).show();
            }
        });

        cancelBtn.setOnClickListener(view -> addCourseDialog.dismiss());
    }

    private void initRecyclerView(String userEmail, String semesterName) {
        RecyclerView courseRecyclerView = findViewById(R.id.sectionListRecyclerView);
        adapter = new AdapterCourseRecycler(this, userEmail, semesterName);
        courseRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        courseRecyclerView.setLayoutManager(layoutManager);
    }
}