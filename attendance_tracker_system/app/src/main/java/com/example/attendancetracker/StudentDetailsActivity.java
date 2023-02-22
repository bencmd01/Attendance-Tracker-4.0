package com.example.attendancetracker;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;

/**     Semester: Fall 2022
 * Displays student information and attendance history for that student
 * Name, email, QR code, calendar view, list history
 *
 * Needs more work, list view is unimplemented (started from previous group)
 *  Calendar needs replaced with customizable view
 */

public class StudentDetailsActivity extends AppCompatActivity {
    private static final String TAG = "STUDENT_VIEW";

    String studentName;
    String studentEmail;
    String courseName;
    String semesterName;
    String userEmail;

    TextView studentNameView, studentEmailView;

    DBHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

        db = new DBHandler(this);

        studentEmail = getIntent().getStringExtra("studentEmail");
        courseName = getIntent().getStringExtra("course");
        semesterName = getIntent().getStringExtra("semester");
        userEmail = getIntent().getStringExtra("userEmail");
        studentName = db.getStudentName(studentEmail, courseName, semesterName, userEmail);
        this.setTitle(courseName);

        studentNameView = (TextView) findViewById(R.id.studentDetailStudentName);
        studentNameView.setText(studentName);
        studentEmailView = (TextView) findViewById(R.id.studentDetailStudentEmail);
        studentEmailView.setText(studentEmail);

        Bitmap myQRCode = QRCodeOperator.generateQRCode(studentEmail);
        ImageView qrCodeView = findViewById(R.id.qrCodeView);
        qrCodeView.setImageBitmap(myQRCode);
    }
}