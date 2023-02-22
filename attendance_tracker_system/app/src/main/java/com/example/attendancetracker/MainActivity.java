package com.example.attendancetracker;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

/**     Semester: Fall 2022
 * MainActivity is the landing page after logging in.
 * The user will find all their semesters and courses here.
 * MainActivity has methods for creating, editing, and deleting
 * semesters and courses.
 *
 * Recycler views are used to list semesters and courses
 * Handles time use permissions.
 */
public class MainActivity extends AppCompatActivity {
    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 23;
    private static final String TAG = "MAIN_ACTIVITY";
    private ArrayList<String> semesterList;

    private DBHandler db;
    private RecyclerView semesterRecyclerView;
    private AdapterSemesterRecycler adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Show the app version number for beta purposes
        //TODO remove this for release
        TextView appVer = (TextView) findViewById(R.id.appVerTextView);
        appVer.setText("Version " + BuildConfig.VERSION_NAME);

        //Request external storage permission if not already allowed
        requestStoragePermission();

        db = new DBHandler(this);

        String userEmail = getIntent().getStringExtra("userEmail");
        Log.d(TAG, db.getUserName(userEmail));

        semesterList = getSemestersFromDB(userEmail);
        if (semesterList == null) {
            semesterList = new ArrayList<>();
        }
        initRecyclerView(userEmail, semesterList);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addSemesterBtn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSemester(userEmail);
            }
        });

    }

    /*
    Initialize the semester list, also contains the course list
    Functionality can be found in AdapterSemesterRecycler and
    AdapterCourseRecycler
     */
    private void initRecyclerView(String userEmail, ArrayList<String> semesterList) {
        semesterRecyclerView = findViewById(R.id.semesterRecyclerView);

        adapter = new AdapterSemesterRecycler(this, userEmail, semesterList);
        // Adapter defines the format and the data bound to the recycler view
        semesterRecyclerView.setAdapter(adapter);
        // layoutManager is a predefined way that the adapter will display
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        semesterRecyclerView.setLayoutManager(layoutManager);
    }

    // Retrieves the list of semesters for this user from the database
    private ArrayList<String> getSemestersFromDB(String email) {
        return db.getSemesters(email);
    }

    // Creates a popup to add a semester and corresponding courses
    private void addSemester(String userEmail) {
        Dialog addSemesterDialog = new Dialog(this);
        LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.dialog_semester_add, null);
        addSemesterDialog.setContentView(layout);
        addSemesterDialog.show();

        EditText semesterNameEdit = (EditText) addSemesterDialog.findViewById(R.id.semesterAddSemesterName);
        EditText courseName1 = (EditText) addSemesterDialog.findViewById(R.id.semesterAddCourseName1);
        EditText courseName2 = (EditText) addSemesterDialog.findViewById(R.id.semesterAddCourseName2);
        EditText courseName3 = (EditText) addSemesterDialog.findViewById(R.id.semesterAddCourseName3);
        EditText courseName4 = (EditText) addSemesterDialog.findViewById(R.id.semesterAddCourseName4);
        EditText courseName5 = (EditText) addSemesterDialog.findViewById(R.id.semesterAddCourseName5);
        Button acceptAddSemesterButton = (Button) addSemesterDialog.findViewById(R.id.semesterAddAcceptButton);
        Button cancelAddSemesterButton = (Button) addSemesterDialog.findViewById(R.id.semesterAddCancelButton);

        acceptAddSemesterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newSemesterName = semesterNameEdit.getText().toString().trim();
                String course1Name = courseName1.getText().toString().trim();
                String course2Name = courseName2.getText().toString().trim();
                String course3Name = courseName3.getText().toString().trim();
                String course4Name = courseName4.getText().toString().trim();
                String course5Name = courseName5.getText().toString().trim();
                ArrayList<String> courses = new ArrayList<>();
                courses.add(course1Name);
                courses.add(course2Name);
                courses.add(course3Name);
                courses.add(course4Name);
                courses.add(course5Name);

                if (newSemesterName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "New semester name cannot be empty.", Toast.LENGTH_LONG).show();
                } else {
                    boolean verifyAdd = db.addSemester(newSemesterName, userEmail);
                    if (verifyAdd) {
                        Toast.makeText(MainActivity.this, "Semester has been added!", Toast.LENGTH_SHORT).show();
                        semesterList.add(newSemesterName);
                        Boolean addedCourses = addCoursesToDB(courses, newSemesterName, userEmail);
                        ArrayList<String> courseSet = db.getCourses(newSemesterName, userEmail);
                        if (courseSet == null) {
                            addSemesterDialog.dismiss();
                            return;
                        }
                        for (String s : courseSet) {
                            Log.d(TAG, "onClick: " + s + " in " + newSemesterName);
                        }
                        addSemesterDialog.dismiss();
                        adapter.notifyDataSetChanged();
                    } else{
                        Toast.makeText(MainActivity.this, "Could not add semester, try again.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        cancelAddSemesterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSemesterDialog.dismiss();
            }
        });
    }

    // Helper method to add all listed courses to database and returns a boolean on completion
    private Boolean addCoursesToDB(ArrayList<String> courses, String semesterName, String userEmail) {
        if (courses.isEmpty()) { return false; };
        for (String s : courses) {
            if (!s.isEmpty()) {
                Boolean result = db.addCourse(s, semesterName, userEmail);
                if (!result) {
                    Toast.makeText(this, "Course could not be added: " + s, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * This method checks if external storage permissions were granted.
     * If not, prompt the user to allow for storage permissions.
     */
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return;

        ActivityCompat.requestPermissions(this, new String[]
                {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, EXTERNAL_STORAGE_PERMISSION_CODE);
    }

}