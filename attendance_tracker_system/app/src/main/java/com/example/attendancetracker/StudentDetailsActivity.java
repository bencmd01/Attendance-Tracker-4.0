package com.example.attendancetracker;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**     Semester: Fall 2022
 * Displays student information and attendance history for that student
 * Name, email, QR code, calendar view, list history
 *
 * Needs more work, list view is unimplemented (started from previous group)
 *  Calendar needs replaced with customizable view
 */

/**     Semester: Spring 2023
 * Changes made:
 * Implemented the ability to send an individual student their QR Code from this screen
 *
 * Attendance grade needs implementation (Ratio of attended to total days in class)
 * Counter for attended days during shown month needs implementation (Ties in to Calendar View)
 * List view remains unimplemented (Unsure of intended function)
 * Calendar View still needs to be replaced with a customizable view. (Could not find a suitable component)
 */

public class StudentDetailsActivity extends AppCompatActivity {
    private static final String TAG = "STUDENT_VIEW";

    String studentName;
    String studentEmail;
    String courseName;
    String semesterName;
    String userEmail;

    String shortQrCodeFilePath;
    String appQRCodeDirectory;

    TextView studentNameView, studentEmailView;

    CalendarView calendarView;

    Button qrButton;

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

        calendarView = (CalendarView) findViewById(R.id.calendarView);

        qrButton = (Button) findViewById(R.id.qrButton);

        shortQrCodeFilePath = "/" + userEmail + "/" + semesterName + "/" + courseName;
        appQRCodeDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + shortQrCodeFilePath;

        studentNameView = (TextView) findViewById(R.id.studentDetailStudentName);
        studentNameView.setText(studentName);
        studentEmailView = (TextView) findViewById(R.id.studentDetailStudentEmail);
        studentEmailView.setText(studentEmail);

        Bitmap myQRCode = QRCodeOperator.generateQRCode(studentEmail);
        ImageView qrCodeView = findViewById(R.id.qrCodeView);
        qrCodeView.setImageBitmap(myQRCode);

        // Defines the action for pressing the Send QR Code button in an individual studen's details.
        qrButton.setOnClickListener(e-> {
            new AlertDialog.Builder(this)
                    .setTitle("Send?")
                    .setMessage("Send QR Code to this student?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            new Thread()
                            {
                                @Override
                                public void run()
                                {
                                    // Creates temp directory for the generated code
                                    File tempDir = new File(appQRCodeDirectory);
                                    if (!tempDir.exists())
                                    {
                                        tempDir.mkdirs();
                                    }

                                    File f = new File(tempDir,"QRCode.png");

                                    // Attempts to generate and send a QR code for whichever student is currently being looked at
                                    try {
                                        Bitmap qr = myQRCode;
                                        f.createNewFile();
                                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                        //Yes, I'm aware this probably isn't the right way to do this.
                                        //Yes, I'm aware that this is a duct tape fix.
                                        //No, I don't think it's a problem.
                                        qr.compress(Bitmap.CompressFormat.PNG,100,bos);
                                        byte[] bmData = bos.toByteArray();
                                        FileOutputStream fos = new FileOutputStream(f);
                                        fos.write(bmData);
                                        fos.flush();
                                        fos.close();
                                        EmailSender sender = new EmailSender();
                                        sender.emailStudentQRCode(courseName, studentName, studentEmail,db.getUserName(userEmail), f);
                                    } catch (IOException ex) {
                                        System.out.println("IOException Caught! File was not created.");
                                        throw new RuntimeException(ex);
                                    } catch (Exception ex) {
                                        System.out.println("Failed to send QR Code, Exception occurred.");
                                        ex.printStackTrace();
                                    }
                                }
                            }.start();
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
        });
    }
}