package com.example.attendancetracker;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**     Semester: Fall 2022
 * SectionViewActivity displays the student list of the course that was selected
 * from MainActivity or CourseActivity
 *
 * This class holds the primary functionality of this application:
 *      Importing class list
 *          Select a CSV file from local storage
 *      Adding students
 *          Add a student with first name, last name, and email
 *      Generating attendance reports
 *          Sends a CSV file containing attendance records to user's email
 *          (Email used at registration and login)
 *      Mailing QR codes to students
 *          Send QR code to student emails for them to use when taking attendance
 *      Scanning for attendance
 *          Scan QR codes
 *      Adding the meeting times for the course
 *          Adds the courses meetings days and times (one time use, for now)
 *      Displaying a list of students
 *          Lists all the students and allows editing, deletion, addition
 *      Navigating to student details page
 *          Displays StudentDetailsActivity screen
 *      Navigating to analytics screen
 *          Displays course overview attendance information (unimplemented)
 */

/**    Semester: Spring 2023
 *     Changes made:
 *      - Added a method initiateQRBatchConfirmation which defines and displays a confirmation dialog when QR codes are sent to all students.
 * */

public class SectionViewActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> fileChooserLauncher;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<String> emailList = new ArrayList<>();
    private ArrayList<String> attendanceList = new ArrayList<>();

    private static final String TAG = "SECTION_VIEW";

    private String appQRCodeDirectory;
    private String publicQRCodeDirectory;

    private ImageButton scanBtn, importBt, generateQRBt, addStudentBt, generateReportBt, analysisBt;
    private Button editTimes;

    private TextView studentTextMessage;
    private SectionViewActivity instance;
    boolean shouldSendQrCodes = true;
    boolean qrCodesSentSuccessfully;

    private String sectionName;
    private String semesterName;
    private String userEmail;
    private String studentEmail;
    private String studentName;

    private RecyclerView studentsRecyclerView;
    private AdapterStudentRecycler adapter;

    DBHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_view);

        db = new DBHandler(this);

        instance = this;
        sectionName = getIntent().getStringExtra("course");
        semesterName = getIntent().getStringExtra("semester");
        userEmail = getIntent().getStringExtra("userEmail");
        String debugString = String.format("COURSE: %s, SEMESTER: %s, USER_EMAIL: %s", sectionName, semesterName, userEmail);
        Log.d(TAG, debugString);

        // Set the file path where QR codes are saved on the device
        String shortQrCodeFilePath = "/" + userEmail + "/" + semesterName + "/" + sectionName;
        appQRCodeDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + shortQrCodeFilePath;
        Log.d("PATH", "appQRCodeDirectory: " + appQRCodeDirectory);
        publicQRCodeDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Attendance Tracker" + shortQrCodeFilePath;
        Log.d("PATH", "publicQRCodeDirectory: " + publicQRCodeDirectory);

        File tempDir = new File(appQRCodeDirectory);
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }

        fileChooserLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                this::importClassList);

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
                    if (result.getContents() == null) {
                        Toast.makeText(SectionViewActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else {
                        String studentEmail = result.getContents();
                        Log.d("SCANNED", studentEmail);
                        String prompt;
                        if (recordAttendance(studentEmail)) {
                            prompt = "Scan successful for: " + db.getStudentName(studentEmail, sectionName, semesterName, userEmail) + " (" + studentEmail + ").";
                        } else {
                            prompt = "Scan unsuccessful.";
                        }
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            barcodeLauncher.launch(makeScanOptions(prompt));
                        }, 1000);
                    }
                });

        setTitle(sectionName);
        initClassListView(userEmail, semesterName, sectionName);

        scanBtn = findViewById(R.id.sectionViewScanButton);
        importBt = findViewById(R.id.importlist);
        generateQRBt = findViewById(R.id.generateBatch);
        addStudentBt = findViewById(R.id.addStudentButton);
        generateReportBt = findViewById(R.id.sectionExportButton);
        analysisBt = findViewById(R.id.sectionAnalyticsButton);
        editTimes = findViewById(R.id.sectionViewTimeEditButton);
        if (db.getCourseMeetingsCount(sectionName, semesterName, userEmail) > 0) {
            editTimes.setText("View Times");
        }
        studentTextMessage = findViewById(R.id.nostudentDisplay);
        if (db.getNumberOfStudentsInClass(sectionName, semesterName, userEmail) > 0) {
            studentTextMessage.setVisibility(View.GONE);
        }

        scanBtn.setOnClickListener(view -> barcodeLauncher.launch(makeScanOptions("Scan a barcode or QR Code")));
        importBt.setOnClickListener(view -> fileChooserLauncher.launch(new String[]{"text/comma-separated-values"}));
        generateQRBt.setOnClickListener(view -> initiateBatchRequest());
        addStudentBt.setOnClickListener(view -> addStudent());
        generateReportBt.setOnClickListener(view -> generateReports());
        analysisBt.setOnClickListener(view -> initiateAnalysisScreen());
        editTimes.setOnClickListener(view -> editMeetingTimes());
    }

    private void initClassListView(String userEmail, String semesterName, String sectionName) {
        studentsRecyclerView = findViewById(R.id.studentListRecyclerView);
        adapter = new AdapterStudentRecycler(this, userEmail, semesterName, sectionName);
        studentsRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        studentsRecyclerView.setLayoutManager(layoutManager);
    }

    public void initiateAnalysisScreen(){
        Intent analysisIntent = new Intent(this, AnalysisScreenActivity.class);
        startActivity(analysisIntent);
    }

    public void initiateBatchRequest(){
        Dialog sendCodeDialog = new Dialog(this);
        ConstraintLayout layout = (ConstraintLayout) View.inflate(this, R.layout.dialog_send_codes, null);
        sendCodeDialog.setContentView(layout);
        sendCodeDialog.show();

        Button sendCodeAcceptButton = (Button) sendCodeDialog.findViewById(R.id.sendCodesAcceptButton);
        Button sendCodeNoButton = (Button) sendCodeDialog.findViewById(R.id.sendCodesNoButton);
        Button sendCodeCancelButton = (Button) sendCodeDialog.findViewById(R.id.sendCodesCancelButton);

        sendCodeAcceptButton.setOnClickListener(view -> {
            shouldSendQrCodes = true;
            new Thread()
            {
                @Override
                public void run() {
                    qrCodesSentSuccessfully = true;
                    try {
                        generateQrCodeBatch();
                    }
                    catch (Exception ex)
                    {
                        qrCodesSentSuccessfully = false;
                        ex.printStackTrace();
                    }
                }
            }.start();
            sendCodeDialog.dismiss();
            initiateQRBatchConfirmation();
        });

        sendCodeNoButton.setOnClickListener(view -> {
            shouldSendQrCodes = false;
            new Thread()
            {
                @Override
                public void run() {
                    try {
                        generateQrCodeBatch();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }.start();
            sendCodeDialog.dismiss();
        });

        sendCodeCancelButton.setOnClickListener(view -> {
            shouldSendQrCodes = false;
            sendCodeDialog.dismiss();
        });

    }

    /**
     * Initiates the dialog that confirms that QR codes were sent based on whether or not the sending was successful.
     * This should appear after "Yes" is selected on "dialog_send_codes".
     */
    public void initiateQRBatchConfirmation(){
        Dialog confirmSentDialog = new Dialog(this);
        ConstraintLayout layout;
        Button confirmSentDialogClose;

        if (qrCodesSentSuccessfully) {
            layout = (ConstraintLayout) View.inflate(this, R.layout.dialog_qr_sent_success, null);
            confirmSentDialogClose = layout.findViewById(R.id.sendCodesSuccessCloseButton);
        }
        else{
            layout = (ConstraintLayout) View.inflate(this, R.layout.dialog_qr_sent_failure, null);
            confirmSentDialogClose = layout.findViewById(R.id.sendCodesFailureCloseButton);
        }

        confirmSentDialog.setContentView(layout);
        confirmSentDialog.show();

        confirmSentDialogClose.setOnClickListener(view -> confirmSentDialog.dismiss());
    }

    // Takes user input and creates a new student with given information
    private void addStudent() {
        Dialog addStudentDialog = new Dialog(this);
        ConstraintLayout layout = (ConstraintLayout) View.inflate(this, R.layout.dialog_student_add, null);
        addStudentDialog.setContentView(layout);
        addStudentDialog.show();

        EditText newFName = (EditText) addStudentDialog.findViewById(R.id.studentFirstNameAdd);
        EditText newLName = (EditText) addStudentDialog.findViewById(R.id.studentLastNameAdd);
        EditText newEmail = (EditText) addStudentDialog.findViewById(R.id.studentEmailAdd);
        Button acceptButton = (Button) addStudentDialog.findViewById(R.id.studentAddAcceptButton);
        Button cancelButton = (Button) addStudentDialog.findViewById(R.id.studentAddCancelButton);

        //Set up the buttons
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean nameValid = true;
                boolean emailValid = false;
                String[] studentNameParts = new String[2];
                studentNameParts[0] = newFName.getText().toString().trim();
                studentNameParts[1] = newLName.getText().toString().trim();
                studentEmail = newEmail.getText().toString().trim();
                if (studentNameParts[0].equals("") && studentNameParts[1].equals("")) {
                    studentName = "null null";
//                    Toast.makeText(SectionViewActivity.this,"Please enter a Student's Name", Toast.LENGTH_SHORT).show();
                }
                else nameValid = true;

                if (studentEmail.equals("")) {
                    Toast.makeText(SectionViewActivity.this,"Please enter a Student's Email", Toast.LENGTH_SHORT).show();
                }
                else emailValid = true;

                if (nameValid & emailValid) {
                    String studentFirst = studentNameParts[0];
                    String studentLast = studentNameParts[1];

                    nameList.add(studentFirst + " " + studentLast);
                    emailList.add(studentEmail);

                    Boolean addedStudent = db.addStudent(studentEmail, sectionName, semesterName, userEmail, studentFirst, studentLast);

                    if (addedStudent) {
                        Toast.makeText(SectionViewActivity.this,
                                String.format("Student %s %s added successfully", studentFirst, studentLast),
                                Toast.LENGTH_LONG).show();
                        addStudentDialog.dismiss();
                    } else {
                        Toast.makeText(SectionViewActivity.this, "Unsuccessful", Toast.LENGTH_SHORT).show();
                    }
                    initClassListView(userEmail, semesterName, sectionName);
                    studentTextMessage.setVisibility(View.GONE);
                }
                else {
                    //Invalid handling
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addStudentDialog.cancel();
            }
        });
    }

    //Returns a size 2 array of times [ HH , MM ]
    //Default is 12:00
    private Integer[] parseTime(String time) {
        String[] timeTokens = {"12", "00"};
        String hour = "12";
        String minute = "00";
        if (time.contains(":")) {
            timeTokens = time.split(":");
        } else if (time.length() == 4) {
            timeTokens[0] = time.substring(0, 2);
            timeTokens[1] = time.substring(2);
        } else if (time.length() == 3) {
            timeTokens[0] = time.substring(0, 1);
            timeTokens[1] = time.substring(1);
        } else if (time.length() == 2) {
            timeTokens[0] = time;
            timeTokens[1] = "00";
        } else if (time.length() == 1) {
            timeTokens[0] = time;
            timeTokens[1] = "00";
        } else if (time.length() > 4) {
            //Time entered shouldn't be larger than 4 digits
            parseTime(time.substring(0, 4));
        }

        if (time.isEmpty()) {
            return new Integer[] {Integer.valueOf(hour), Integer.valueOf(minute) };
        }

        if (timeTokens.length > 2) {
            //Error, can't have more than 1 ':'
        } else if (timeTokens.length == 2) {
            hour = timeTokens[0];
            minute = timeTokens[1];

            if (Integer.parseInt(hour) > 12 || Integer.parseInt(hour) < 1) {
                //Error, hour shouldn't be larger than 12 or smaller than 1
                int hourInt = Integer.parseInt(hour);
                hourInt = hourInt%12;
                if (hourInt == 0) hourInt = 12;
                hour = Integer.toString(hourInt);
            }
            if (Integer.parseInt(minute) > 59 || Integer.parseInt(minute) < 0) {
                //Error, minute shouldn't be larger than 59 or smaller than 0
            }

        } else if (timeTokens.length == 1) {
            hour = timeTokens[0];
            minute = "00";

            if (Integer.parseInt(hour) > 12 || Integer.parseInt(hour) < 1) {
                //Error, hour shouldn't be larger than 12 or smaller than 1
                int hourInt = Integer.parseInt(hour);
                hourInt = hourInt%12;
                if (hourInt == 0) hourInt = 12;
                hour = Integer.toString(hourInt);
            }
        }

        if (hour.isEmpty()) {hour = "12";}
        if (minute.isEmpty()) {minute = "00";}

        return new Integer[] {Integer.valueOf(hour), Integer.valueOf(minute) };
    }

    /**
     * Allow the user to select a CSV file from external storage. The method will read through
     * the file, getting students from a line's fields and adding students to the database and
     * student list.
     */
    private void importClassList(Uri uri) {

        boolean result = true;

        if (uri == null) {
            return;
        }
        Log.d(TAG, "Path: " + uri.getPath());
        try {
            // Create BufferedReader to read content from the selected CSV file.
            InputStream inputStream = this.getContentResolver().openInputStream(uri);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";

            // Read fields from each line where each line is in the format:
            // firstName,lastName,email
            while ((line = buffer.readLine()) != null) {
                Log.d("LINE", line);
                String[] fields = line.split(",", 4);
                if (fields.length < 3) {
                    // Error formatting if number of fields in a line is different from 3.
                    Log.e("CSV","Formatting error: Must have 3 fields in each row.");
                    Toast.makeText(SectionViewActivity.this, "Error reading from file.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Get fields and store into variables.
                String studentFirstName = fields[0].trim();
                String studentLastName = fields[1].trim();
                String studentEmail = fields[2].trim();
                Log.d(TAG, String.format("Line: %s %s %s", studentFirstName, studentLastName, studentEmail));

                // If the student does not already exist, add the student.
                Boolean addedStudent = false;
                if (db.getStudentName(studentEmail, sectionName, semesterName, userEmail) == null) {
                    addedStudent = db.addStudent(studentEmail, sectionName, semesterName, userEmail, studentFirstName, studentLastName);
                }

                // If adding the student fails, update result.
                if (!addedStudent) {
                    result = false;
                } else {
                    // Hide the "No students added" message if it is visible.
                    studentTextMessage.setVisibility(View.GONE);
                }

                // Update student list.
                initClassListView(userEmail, semesterName, sectionName);
            }
            buffer.close();
            inputStream.close();

            // Produce success/failure message
            if (result) {
                Toast.makeText(SectionViewActivity.this, "All students successfully added from file.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SectionViewActivity.this, "Error importing one or more students: Duplicate student(s).",
                        Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(SectionViewActivity.this, "Error reading from file.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Constructs the options for the QR code scanner (used when launching the barcodeLauncher).
     */
    private ScanOptions makeScanOptions(String prompt) {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(prompt);
        options.setOrientationLocked(false);
		options.setCameraId(1);
        return options;
    }

    /**
     * Returns true if 1) a student with the given student email exists in the database and
     * 2) the date present for the student was successfully recorded in the database.
     * Returns false otherwise.
     */
    private boolean recordAttendance(String studentEmail) {
        return (db.getStudentName(studentEmail, sectionName, semesterName, userEmail) != null) &&
                (db.addDatePresent(studentEmail, sectionName, semesterName, userEmail, getCurrentDate()));
    }

    private Date getCurrentDate() {
        return Calendar.getInstance().getTime();
    }

    /**
     * This displays a pop-up window allowing the user to enter a date that will be used in
     * report generation. This allows the user to input the date either through the keyboard
     * or through a date picker. The pop-up disappears if the user selects “cancel” or “accept.”
     * If the user selects “accept,” then attendance reports will be send to the user’s email.
     */
    private void generateReports() {
        Dialog reportDialog = new Dialog(this);
        ConstraintLayout layout = (ConstraintLayout) View.inflate(this, R.layout.dialog_attendance_report, null);
        reportDialog.setContentView(layout);
        reportDialog.show();

        EditText generateReportDateText = (EditText) reportDialog.findViewById(R.id.attendanceReportDateText);
        ImageButton generateReportDatePickerButton = (ImageButton) reportDialog.findViewById(R.id.attendanceReportDatePickerButton);
        Button generateReportAcceptButton = (Button) reportDialog.findViewById(R.id.attendanceReportAcceptButton);
        Button generateReportCancelButton = (Button) reportDialog.findViewById(R.id.attendanceReportCancelButton);

        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);

                // After choosing a date, set the EditText's text to that date.
                String format = "yyyy-MM-dd";
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                generateReportDateText.setText(sdf.format(calendar.getTime()));
            }
        };
        generateReportDatePickerButton.setOnClickListener(view -> {
            // Show a date picker where the user can select a date to input into the EditText
            new DatePickerDialog(SectionViewActivity.this, date, calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        generateReportAcceptButton.setOnClickListener(view -> {
            try {
                // Write and send reports and dismiss the pop-up
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date chosenDate = sdf.parse(generateReportDateText.getText().toString());
                writeReports(chosenDate);
                reportDialog.dismiss();
            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(SectionViewActivity.this,"Please enter a date in the format: YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            }
        });
        generateReportCancelButton.setOnClickListener(view -> {
            // dismiss the pop-up
            reportDialog.dismiss();
        });
    }

    /**
     * Creates attendance reports based on the date in the database. These files are
     * stored in the device as CSV files. The reports are also mailed to the user via
     * the user's email.
     */
    private void writeReports(Date chosenDate)
    {
        // print chosenDate for testing
        Log.d(TAG, "chosenDate: " + chosenDate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String formattedDate = sdf.format(chosenDate);
        Log.d(TAG, "formattedDate: " + formattedDate);

        try {
            // Initialize device storage
            String pathname = getExternalFilesDir(null).getAbsolutePath() + "/Reports/" + semesterName + "/" + sectionName;
            Log.d("PATHNAME", "pathname: " + pathname);

            File tempDir = new File(pathname);
            if (!tempDir.mkdirs()) {
                if (!tempDir.exists()) {
                    throw new Exception("Failed to create the folder for reports.");
                }
            }

            // Don't generate reports if there are no students.
            ArrayList<String> studentEmails = db.getStudents(sectionName, semesterName, userEmail);
            if (studentEmails == null) {
                Toast.makeText(SectionViewActivity.this, "There are no students.", Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(TAG, "Students: " + studentEmails);

            // Old all attendance report - client said this information was not that useful to her.
//            String allAttendanceFilename = "/All Attendance Report.csv";
//            String[] allAttendanceHeader = {"First_Name", "Last_Name", "Email", "Date"};
//
//            File allAttendanceFile = new File(pathname, allAttendanceFilename);
//            FileWriter allAttendanceFileWriter = new FileWriter(allAttendanceFile);
//            CSVWriter allAttendanceCSVWriter = new CSVWriter(allAttendanceFileWriter);
//
//            // Build CSV file with all attendance for this class
//            allAttendanceCSVWriter.writeNext(allAttendanceHeader);
//            for (String studentEmail : studentEmails) {
//                ArrayList<String[]> studentDates = db.getDatesPresent(studentEmail, sectionName, semesterName, userEmail);
//                if (studentDates != null) {
//                    for (int index = 0; index < studentDates.size(); index++) {
//                        if (studentDates.get(index) != null) {
//                            String[] date = studentDates.get(index);
//                            date[3] = date[3].split(" ")[0];
//                            allAttendanceCSVWriter.writeNext(date);
//                        }
//                    }
//                }
//            }
//            allAttendanceCSVWriter.close();
//            allAttendanceFileWriter.close();

            // The 1st report contains all past meeting dates and the students present
            // and absent on each meeting date.
            String fullAttendanceFilename = "/Full Attendance Report.csv";
            String[] fullAttendanceHeader = {"Date", "Present/Absent", "Last_Name First_Name Email", "..."};

            // Create CSVWriter
            File fullAttendanceFile = new File(pathname, fullAttendanceFilename);
            FileWriter fullAttendanceFileWriter = new FileWriter(fullAttendanceFile);
            CSVWriter fullAttendanceCSVWriter = new CSVWriter(fullAttendanceFileWriter);

            //Write header
            fullAttendanceCSVWriter.writeNext(fullAttendanceHeader);
            ArrayList<String> classDates = db.getPastCourseDates(sectionName, semesterName, userEmail);
            if (classDates == null) {
                // If there are no dates recorded, then output such in the report.
                fullAttendanceCSVWriter.writeNext(new String[]{"No student attendances have been recorded yet."});
            } else {
                for (String date : classDates) {
                    // For each date, print the students present on that date on the same line.
                    ArrayList<String[]> studentsPresentOnDate = db.getStudentsPresentOnDate(
                            date, sectionName, semesterName, userEmail);
                    String[] allPresentStudents = new String[studentsPresentOnDate.size() + 2];
                    allPresentStudents[0] = date;
                    allPresentStudents[1] = "Present";

                    for (int index = 0; index < studentsPresentOnDate.size(); index++) {
                        String[] currentStudent = studentsPresentOnDate.get(index);
                        allPresentStudents[index + 2] = currentStudent[0] + " " + currentStudent[1]
                                + " " + currentStudent[2];
                    }
                    fullAttendanceCSVWriter.writeNext(allPresentStudents);

                    // For each date, print the students absent on that date on the same line.
                    ArrayList<String[]> studentsAbsentOnDate = db.getStudentsAbsentOnDate(
                            date, sectionName, semesterName, userEmail);
                    if (studentsAbsentOnDate == null) {
                        fullAttendanceCSVWriter.writeNext(new String[]{date, "Absent", "None"});
                    } else {
                        String[] allAbsentStudents = new String[studentsAbsentOnDate.size() + 2];
                        allAbsentStudents[0] = date;
                        allAbsentStudents[1] = "Absent";

                        for (int index = 0; index < studentsAbsentOnDate.size(); index++) {
                            String[] currentStudent = studentsAbsentOnDate.get(index);
                            allAbsentStudents[index + 2] = currentStudent[0] + " " + currentStudent[1]
                                    + " " + currentStudent[2];
                        }
                        fullAttendanceCSVWriter.writeNext(allAbsentStudents);
                    }
                }
            }
            fullAttendanceCSVWriter.close();
            fullAttendanceFileWriter.close();

            // The 2nd report contains the requested meeting date and the students present
            // and absent on the meeting date.
            String chosenDayAttendanceFilename = "/" + formattedDate + " Attendance Report.csv";
            String[] chosenDayAttendanceHeader = {"Date", "Present/Absent", "Last_Name First_Name Email", "..."};

            // Create CSVWriter
            File chosenDayAttendanceFile = new File(pathname, chosenDayAttendanceFilename);
            FileWriter chosenDayAttendanceFileWriter = new FileWriter(chosenDayAttendanceFile);
            CSVWriter chosenDayAttendanceCSVWriter = new CSVWriter(chosenDayAttendanceFileWriter);

            // Write header
            chosenDayAttendanceCSVWriter.writeNext(chosenDayAttendanceHeader);
            ArrayList<String[]> studentsPresentOnChosenDate = db.getStudentsPresentOnDate(
                    formattedDate, sectionName, semesterName, userEmail);
            if (studentsPresentOnChosenDate == null) {
                // If there are no students present on that date, report the information to the file.
                chosenDayAttendanceCSVWriter.writeNext(new String[]{formattedDate, "Present", "None"});
            } else {
                // Print the students present on the given date on the same line.
                String[] allPresentStudents = new String[studentsPresentOnChosenDate.size() + 2];
                allPresentStudents[0] = formattedDate;
                allPresentStudents[1] = "Present";

                for (int index = 0; index < studentsPresentOnChosenDate.size(); index++) {
                    String[] currentStudent = studentsPresentOnChosenDate.get(index);
                    allPresentStudents[index + 2] = currentStudent[0] + " " + currentStudent[1]
                            + " " + currentStudent[2];
                }
                chosenDayAttendanceCSVWriter.writeNext(allPresentStudents);
            }

            // Print the students absent on the given date on the same line.
            ArrayList<String[]> studentsAbsentOnChosenDate = db.getStudentsAbsentOnDate(
                    formattedDate, sectionName, semesterName, userEmail);
            if (studentsAbsentOnChosenDate == null) {
                chosenDayAttendanceCSVWriter.writeNext(new String[]{formattedDate, "Absent", "None"});
            } else {
                String[] allAbsentStudents = new String[studentsAbsentOnChosenDate.size() + 2];
                allAbsentStudents[0] = formattedDate;
                allAbsentStudents[1] = "Absent";

                for (int index = 0; index < studentsAbsentOnChosenDate.size(); index++) {
                    String[] currentStudent = studentsAbsentOnChosenDate.get(index);
                    allAbsentStudents[index + 2] = currentStudent[0] + " " + currentStudent[1]
                            + " " + currentStudent[2];
                }
                chosenDayAttendanceCSVWriter.writeNext(allAbsentStudents);
            }
            chosenDayAttendanceCSVWriter.close();
            chosenDayAttendanceFileWriter.close();

            // The 3rd report contains the last date every student attended the class.
            String lastDateAttendedFilename = "/Last Date Attended Report.csv";
            String[] lastDateAttendedHeader = {"Last_Name", "First_Name", "Email", "Date"};

            // Create CSVWriter
            File lastDateAttendedFile = new File(pathname, lastDateAttendedFilename);
            FileWriter lastDateAttendedFileWriter = new FileWriter(lastDateAttendedFile);
            CSVWriter lastDateAttendedCSVWriter = new CSVWriter(lastDateAttendedFileWriter);

            // Write header
            lastDateAttendedCSVWriter.writeNext(lastDateAttendedHeader);
            for (String studentEmail : studentEmails) {
                // Get last date attended for each student
                String lastDateAttended = db.getLastRecordedDate(studentEmail,
                        sectionName, semesterName, userEmail);

                // Get student first and last name
                String[] firstAndLastName = db.getStudentName(studentEmail,
                        sectionName, semesterName, userEmail).split(" ");
                String[] line = new String[4];
                line[0] = firstAndLastName[1];
                line[1] = firstAndLastName[0];
                line[2] = studentEmail;
                // If a student has never been present, print that information to the file.
                // Otherwise, print the last date attended for the student.
                if (lastDateAttended == null) {
                    line[3] = "No attendance recorded.";
                } else {
                    line[3] = lastDateAttended;
                }
                lastDateAttendedCSVWriter.writeNext(line);
            }
            lastDateAttendedCSVWriter.close();
            lastDateAttendedFileWriter.close();

            // In a separate thread, send the reports to the user.
            new Thread()
            {
                @Override
                public void run()
                {
                    sendReports(fullAttendanceFile, chosenDayAttendanceFile, lastDateAttendedFile);
                }
            }.start();

            // Success message
            Toast.makeText(SectionViewActivity.this, "Reports sent successfully!", Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Constructs an email with the CSV attendance report files and sends it to the user's email.
     */
    private synchronized void sendReports(File... reportFiles)
    {
        try {
            EmailSender sender = new EmailSender();
            sender.emailInstructorReports(semesterName, sectionName, userEmail, db.getUserName(userEmail), reportFiles);
            Log.d("EMAIL", "to " + userEmail);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(SectionViewActivity.this, "Failed to send reports.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Generates QR codes based on students in the database and stores them to the device's
     * application data. Then, if the user specified that it wants to send QR codes to
     * students, it sends QR codes to them via their emails.
     */
    @WorkerThread
    private void generateQrCodeBatch() {
        instance.runOnUiThread(() -> {
            try {
                //Initialize device storage
                File tempDir = new File(appQRCodeDirectory);

                if (!tempDir.mkdirs()) {
                    if (!tempDir.exists()) {
                        Toast.makeText(SectionViewActivity.this, "Generation of QR Batch failed.", Toast.LENGTH_LONG).show();
                        throw new Exception("Failed to create the folder for QR codes.");
                    }
                }

                //Begin to build the QR codes
                ArrayList<String> studentNamesInCurrentSection = db.getAllStudentNames(sectionName, semesterName, userEmail);
                ArrayList<String> studentEmailsInCurrentSection = db.getStudents(sectionName, semesterName, userEmail);

                //Double check that the emails and names of the students in the section line up. If they
                //don't, throw an exception.
                if (studentNamesInCurrentSection.size() == studentEmailsInCurrentSection.size()) {
                    for (int studentIndex = 0; studentIndex < studentEmailsInCurrentSection.size(); studentIndex++) {
                        Bitmap studentQrCode = QRCodeOperator.generateQRCode(studentEmailsInCurrentSection.get(studentIndex));
                        String qrCodeFileName = studentNamesInCurrentSection.get(studentIndex) + ", " + studentEmailsInCurrentSection.get(studentIndex) + ".png";
                        File newQrCode = new File(tempDir, qrCodeFileName);

                        //If the file somehow already exists, delete it before overwriting.
                        if (newQrCode.exists()) {
                            newQrCode.delete();
                        }

                        //Open the file for streaming.
                        FileOutputStream outputStream = new FileOutputStream(newQrCode);
                        try {
                            studentQrCode.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                            outputStream.flush();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        outputStream.close();

                        // Attempt to send the QR Code via email
                        if (shouldSendQrCodes) {
                            int finalStudentIndex = studentIndex;

                            new Thread()
                            {
                                @Override
                                public void run()
                                {
                                    sendQrCodes(studentNamesInCurrentSection.get(finalStudentIndex),
                                                studentEmailsInCurrentSection.get(finalStudentIndex),
                                                newQrCode);
                                }
                            }.start();
                        }
                    }

                } else {
                    throw new Exception("There is an unequal amount of student names and emails in this course section.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void addPicToGallery(Bitmap bitmap, String image_name) throws Exception {
        File myDir = new File(publicQRCodeDirectory);
        if (!myDir.mkdirs()) {
            if (!myDir.exists()) {
                throw new Exception("Failed to create the folder for QR codes.");
            }
        }

        File file = new File(myDir, image_name);
        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.getPath()}, new String[]{"image/png"}, null);
    }

    /**
     * This constructs an email with the student’s QR code and sends it to the student’s email.
     */
    private synchronized void sendQrCodes(String studentName, String studentEmail, File qrCode)
    {
        try {
            EmailSender sender = new EmailSender();
            sender.emailStudentQRCode(sectionName, studentName, studentEmail, db.getUserName(userEmail), qrCode);
            Log.d("EMAIL", "to " + studentEmail);
//            Toast.makeText(SectionViewActivity.this, "QR Code sent Successfully.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(SectionViewActivity.this, "QR Code sending failed.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void editMeetingTimes() {
        Dialog meetingTimeDialog = new Dialog(instance);
        ConstraintLayout layout = (ConstraintLayout) View.inflate(instance, R.layout.dialog_course_meeting_times, null);
        meetingTimeDialog.setContentView(layout);
        meetingTimeDialog.show();

        ArrayList<String> meetDays = null, startHours = null, startMinutes = null, endHours = null, endMinutes = null;
        Button acceptBtn = (Button) meetingTimeDialog.findViewById(R.id.courseMeetingAcceptButton);
        Button cancelBtn = (Button) meetingTimeDialog.findViewById(R.id.courseMeetingCancelButton);

        ArrayList<ArrayList<String>> allArrays = db.getMeetings(sectionName, semesterName, userEmail);
        if (allArrays != null) {
            meetDays = allArrays.get(0);
            startHours = allArrays.get(1);
            startMinutes = allArrays.get(2);
            endHours = allArrays.get(3);
            endMinutes = allArrays.get(4);
            acceptBtn.setVisibility(View.GONE);
            cancelBtn.setText("Exit");
        }

        TextView courseNameView = (TextView) meetingTimeDialog.findViewById(R.id.courseMeetingCourseName);
        courseNameView.setText(sectionName);

        CheckBox sundayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingSundayCheck);
        CheckBox mondayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingMondayCheck);
        CheckBox tuesdayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingTuesdayCheck);
        CheckBox wednesdayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingWednesdayCheck);
        CheckBox thursdayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingThursdayCheck);
        CheckBox fridayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingFridayCheck);
        CheckBox saturdayCheck = (CheckBox) meetingTimeDialog.findViewById(R.id.courseMeetingSaturdayCheck);

        EditText sunStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingSundayStartTime);
        EditText sunEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingSundayEndTime);
        EditText monStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingMondayStartTime);
        EditText monEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingMondayEndTime);
        EditText tuesStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingTuesdayStartTime);
        EditText tuesEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingTuesdayEndTime);
        EditText wedStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingWednesdayStartTime);
        EditText wedEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingWednesdayEndTime);
        EditText thursStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingThursdayStartTime);
        EditText thursEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingThursdayEndTime);
        EditText friStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingFridayStartTime);
        EditText friEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingFridayEndTime);
        EditText satStart = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingSaturdayStartTime);
        EditText satEnd = (EditText) meetingTimeDialog.findViewById(R.id.courseMeetingSaturdayEndTime);

        // If meet days are already in the database, check the boxes
        if (meetDays != null) {
            for (int i = 0; i < meetDays.size(); i++) {
                String day = meetDays.get(i);
                if (day.equals("Sunday")) {
                    sundayCheck.setChecked(true);
                    sunStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    sunEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
                if (day.equals("Monday")) {
                    mondayCheck.setChecked(true);
                    monStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    monEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
                if (day.equals("Tuesday")) {
                    tuesdayCheck.setChecked(true);
                    tuesStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    tuesEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
                if (day.equals("Wednesday")) {
                    wednesdayCheck.setChecked(true);
                    wedStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    wedEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
                if (day.equals("Thursday")) {
                    thursdayCheck.setChecked(true);
                    thursStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    thursEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
                if (day.equals("Friday")) {
                    fridayCheck.setChecked(true);
                    friStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    friEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
                if (day.equals("Saturday")) {
                    saturdayCheck.setChecked(true);
                    satStart.setText(startHours.get(i) + ":" + startMinutes.get(i));
                    satEnd.setText(endHours.get(i) + ":" + endMinutes.get(i));
                }
            }
        }

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTimes.setText("View Times");
                if (sundayCheck.isChecked()) {
                    Integer[] startTime = parseTime(sunStart.getText().toString());
                    Integer[] endTime = parseTime(sunEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Sunday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                if (mondayCheck.isChecked()) {
                    Integer[] startTime = parseTime(monStart.getText().toString());
                    Integer[] endTime = parseTime(monEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Monday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                if (tuesdayCheck.isChecked()) {
                    Integer[] startTime = parseTime(tuesStart.getText().toString());
                    Integer[] endTime = parseTime(tuesEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Tuesday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                if (wednesdayCheck.isChecked()) {
                    Integer[] startTime = parseTime(wedStart.getText().toString());
                    Integer[] endTime = parseTime(wedEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Wednesday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                if (thursdayCheck.isChecked()) {
                    Integer[] startTime = parseTime(thursStart.getText().toString());
                    Integer[] endTime = parseTime(thursEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Thursday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                if (fridayCheck.isChecked()) {
                    Integer[] startTime = parseTime(friStart.getText().toString());
                    Integer[] endTime = parseTime(friEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Friday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                if (saturdayCheck.isChecked()) {
                    Integer[] startTime = parseTime(satStart.getText().toString());
                    Integer[] endTime = parseTime(satEnd.getText().toString());
                    db.addMeeting(sectionName, semesterName, userEmail, "Saturday",
                            startTime[0], startTime[1], endTime[0], endTime[1]);
                }
                meetingTimeDialog.dismiss();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                meetingTimeDialog.dismiss();
            }
        });
    }
}
