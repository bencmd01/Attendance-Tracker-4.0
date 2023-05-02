package com.example.attendancetracker;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for the database
 * Entire database is handled here, including the construction of tables,
 * adding, deleting, and editing data, and other related functionality
 *
 * Refer to URL for information
 https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper
 *
 * Be sure to follow version control conventions, for it could break the application
 * if not followed.
 * i.e. Use onUpgrade method carefully and be sure to increment VERSION when adding to
 *      onUpgrade method.
 *      (This does not need to be done when making new query methods)
 */

public class DBHandler extends SQLiteOpenHelper {
    public static final String DB_NAME = "AttendanceTracker.db";
    private static final int VERSION = 5; // Increment on every database change, make changes in onUpgrade method

    public DBHandler(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    // Allows FK support, (e.g. ON DELETE CASCADE)
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    // Creates necessary tables
    // TABLES: users, semesters, courses, meetings, students, datePresent
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(" +
                "lecturerEmail TEXT Primary Key, " +
                "password TEXT, " +
                "fName TEXT," +
                "lName TEXT)");
        db.execSQL("CREATE TABLE semesters(" +
                "semesterName TEXT," +
                "lecturerEmail TEXT," +
                "PRIMARY KEY (semesterName, lecturerEmail)," +
                "FOREIGN KEY (lecturerEmail)" +
                "   REFERENCES users (lecturerEmail)" +
                "       ON DELETE CASCADE" +
                "       ON UPDATE CASCADE)");
        db.execSQL("CREATE TABLE courses(" +
                "courseName TEXT," +
                "semesterName TEXT," +
                "lecturerEmail TEXT," +
                "PRIMARY KEY (courseName, semesterName, lecturerEmail)," +
                "FOREIGN KEY (semesterName, lecturerEmail)" +
                "   REFERENCES semesters (semesterName, lecturerEmail)" +
                "       ON DELETE CASCADE" +
                "       ON UPDATE CASCADE)");
        db.execSQL("CREATE TABLE meetings(" +
                "courseName TEXT," +
                "semesterName TEXT," +
                "lecturerEmail TEXT," +
                "weekday TEXT," +
                "sHour INTEGER," +
                "sMinute INTEGER," +
                "eHour INTEGER," +
                "eMinute INTEGER," +
                "PRIMARY KEY (courseName, semesterName, lecturerEmail, weekday)," +
                "FOREIGN KEY (courseName, semesterName, lecturerEmail)" +
                "   REFERENCES courses (courseName, semesterName, lecturerEmail)" +
                "       ON DELETE CASCADE" +
                "       ON UPDATE CASCADE)");
        db.execSQL("CREATE TABLE students(" +
                "studentEmail TEXT," +
                "courseName TEXT," +
                "semesterName TEXT," +
                "lecturerEmail TEXT," +
                "fName TEXT," +
                "lName TEXT," +
                "PRIMARY KEY (studentEmail, courseName, semesterName, lecturerEmail)," +
                "FOREIGN KEY (courseName, semesterName, lecturerEmail)" +
                "   REFERENCES courses (courseName, semesterName, lecturerEmail)" +
                "       ON DELETE CASCADE" +
                "       ON UPDATE CASCADE)");
        db.execSQL("CREATE TABLE datesPresent(" +
                "studentEmail TEXT," +
                "courseName TEXT," +
                "semesterName TEXT," +
                "lecturerEmail TEXT," +
                "recordedDate TEXT," +
                "PRIMARY KEY (studentEmail, courseName, semesterName, lecturerEmail, recordedDate)," +
                "FOREIGN KEY (studentEmail, courseName, semesterName, lecturerEmail)" +
                "   REFERENCES students (studentEmail, courseName, semesterName, lecturerEmail)" +
                "       ON DELETE CASCADE" +
                "       ON UPDATE CASCADE)");
    }

    /*
    onUpgrade is used when altering the database schema.
    When changing anything structurally in the database, increment the
        version number VERSION above by 1
    Use this syntax:
        if (oldVersion < <New_Version_Number>) { do changes }
    This new update module should be below the previous one.
    Also, make those same changes in the onCreate method.
    Warning: If this is not done, the user would have to reinstall and lose all data
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Version 4 adds a primary key to datesPresent table, mistakenly not part of it before.
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE datesPresentCopy(" +
                    "studentEmail TEXT," +
                    "courseName TEXT," +
                    "semesterName TEXT," +
                    "lecturerEmail TEXT," +
                    "recordedDate TEXT," +
                    "PRIMARY KEY (studentEmail, courseName, semesterName, lecturerEmail, recordedDate)," +
                    "FOREIGN KEY (studentEmail, courseName, semesterName, lecturerEmail)" +
                    "   REFERENCES students (studentEmail, courseName, semesterName, lecturerEmail)" +
                    "       ON DELETE CASCADE" +
                    "       ON UPDATE CASCADE)");

            db.execSQL("INSERT INTO datesPresentCopy (studentEmail, courseName, semesterName, lecturerEmail, recordedDate)" +
                    "   SELECT studentEmail, courseName, semesterName, lecturerEmail, recordedDate FROM datesPresent");

            db.execSQL("DROP TABLE datesPresent");

            db.execSQL("ALTER TABLE datesPresentCopy RENAME TO datesPresent");
        }
        // Fixed primary key on meetings table
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE meetingsCopy(" +
                    "courseName TEXT," +
                    "semesterName TEXT," +
                    "lecturerEmail TEXT," +
                    "weekday TEXT," +
                    "sHour INTEGER," +
                    "sMinute INTEGER," +
                    "eHour INTEGER," +
                    "eMinute INTEGER," +
                    "PRIMARY KEY (courseName, semesterName, lecturerEmail, weekday)," +
                    "FOREIGN KEY (courseName, semesterName, lecturerEmail)" +
                    "   REFERENCES courses (courseName, semesterName, lecturerEmail)" +
                    "       ON DELETE CASCADE" +
                    "       ON UPDATE CASCADE)");

            db.execSQL("INSERT INTO meetingsCopy (courseName, semesterName, lecturerEmail, weekday, " +
                    "sHour, sMinute, eHour, eMinute)" +
                    "SELECT courseName, semesterName, lecturerEmail, weekday, sHour, sMinute, eHour, eMinute " +
                    "FROM meetings");

            db.execSQL("DROP TABLE meetings");
            db.execSQL("ALTER TABLE meetingsCopy RENAME TO meetings");
        }

    }

    //
    /*
    ADD METHODS SECTION
     */
    //

    // When user registers, takes field information and stores in the database
    public Boolean addUser(String lEmail, String password, String fName, String lName) {
        //Open the database
        SQLiteDatabase db = this.getWritableDatabase();

        //Initialize the content pointer for the added information
        ContentValues values = new ContentValues();

        values.put("lecturerEmail", lEmail);
        values.put("password", password);
        values.put("fName", fName);
        values.put("lName", lName);

        //Add the new information into the table
        long result = db.insert("users", null, values);
        db.close();

        return result != -1;
    }

    //Adds a new row to semesters table
    public Boolean addSemester(String semesterName, String lEmail) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("semesterName", semesterName);
        values.put("lecturerEmail", lEmail);

        long result = db.insert("semesters", null, values);
        db.close();

        return result != -1;
    }

    //Adds a new row to courses table
    public Boolean addCourse(String courseName, String semesterName, String lEmail) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("courseName", courseName);
        values.put("semesterName", semesterName);
        values.put("lecturerEmail", lEmail);

        long result = db.insert("courses", null, values);
        db.close();

        return result != -1;
    }

    //Adds a new row to meetings table
    public void addMeeting(String courseName, String semesterName, String lEmail, String weekDay,
                           int startHour, int startMinute, int endHour, int endMinute) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("courseName", courseName);
        values.put("semesterName", semesterName);
        values.put("lecturerEmail", lEmail);
        values.put("weekday", weekDay);
        values.put("sHour", startHour);
        values.put("sMinute", startMinute);
        values.put("eHour", endHour);
        values.put("eMinute", endMinute);

        db.insert("meetings", null, values);
        db.close();

    }

    //Adds a new row to students table
    public Boolean addStudent(String sEmail, String courseName, String semesterName, String lEmail,
                              String fName, String lName) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("studentEmail", sEmail);
        values.put("courseName", courseName);
        values.put("semesterName", semesterName);
        values.put("lecturerEmail", lEmail);
        values.put("fName", fName);
        values.put("lName", lName);

        long result = db.insert("students", null, values);
        db.close();

        return result != -1;
    }

    //Adds new row to datePresent table
    // Date format yyyy-MM-dd HH:mm:ss
    public Boolean addDatePresent(String sEmail, String courseName, String semesterName, String lEmail,
                                  Date date) {
        SQLiteDatabase db = this.getWritableDatabase();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        ContentValues values = new ContentValues();
        values.put("studentEmail", sEmail);
        values.put("courseName", courseName);
        values.put("semesterName", semesterName);
        values.put("lecturerEmail", lEmail);
        values.put("recordedDate", dateFormat.format(date));

        long result = db.insert("datesPresent", null, values);
        db.close();

        return result != -1;
    }

    //
    /*
    AUTHENTICATE METHODS SECTION
     */
    //

    //Checks if the email for a new user already exists.
    public Boolean checkEmailExist(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE lecturerEmail = ?",
                new String[]{email});

        Boolean result = cursor.getCount() == 1;
        cursor.close();
        db.close();
        return result;
    }

    // Authenticate for logging in, will check for any rows in the user table
    //  where the email and password match
    public Boolean checkEmailPassword(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE lecturerEmail = ? AND password = ?",
                new String[]{email, password});
        // True if match, false otherwise
        Boolean result = cursor.getCount() == 1;
        cursor.close();
        db.close();
        return result;
    }

    //
    /*
    GET METHODS SECTION
     */
    //

    // Returns the name as a single string (First Last) using the user's email
    public String getUserName(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT fName, lName FROM users WHERE lecturerEmail = ?",
                new String[]{userEmail});
        if (cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();

        String name = cursor.getString(0) + " " + cursor.getString(1);

        cursor.close();
        db.close();
        return name;
    }

    // Returns a list of all semesters associated with the user
    public ArrayList<String> getSemesters(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM semesters WHERE lecturerEmail = ?",
                new String[]{userEmail});
        if (cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();

        ArrayList<String> semesters = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            String semesterName = cursor.getString(0);
            semesters.add(semesterName);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return semesters;
    }

    // Returns a list of courses that are associated with the current user and semester
    public ArrayList<String> getCourses(String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM courses WHERE semesterName = ? AND lecturerEmail = ?",
                new String[]{semester, userEmail});
        if (cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();

        ArrayList<String> courses = new ArrayList<>();

        while (!cursor.isAfterLast()) {
            String courseName = cursor.getString(0);
            courses.add(courseName);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return courses;
    }

    // Returns a list of the class's marked meeting days as a list
    // Each internal list represents one day of the week
    // i.e., a class that meets three times a week will have 3 internal lists
    // Each internal list represents the meeting day as so: [DOW, start_hour, start_minute, end_hour, end_minute]
    public ArrayList<ArrayList<String>> getMeetings(String courseName, String semester, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM meetings WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[]{courseName, semester, userEmail});
        if (cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();

        /*
        Parallel arraylists where each index aligns
        Example:
            meetingDays[0] == Monday     meetingDays[1] == Wednesday
            meetingStartHour[0] == 3     meetingStartHour[1] == 3
            meetingStartMinute[0] == 15  meetingStartMinute[1] == 15
            meetingEndHour[0] == 4       meetingEndHour[1] == 4
            meetingEndMinute[0] == 30    meetingEndMinute[1] == 30

            Represents a course whose meeting times are mondays and wednesdays
            at 3:15 until 4:30
         */
        ArrayList<String> meetingDays = new ArrayList<>();
        ArrayList<String> meetingStartHour = new ArrayList<>();
        ArrayList<String> meetingStartMinute = new ArrayList<>();
        ArrayList<String> meetingEndHour = new ArrayList<>();
        ArrayList<String> meetingEndMinute = new ArrayList<>();

        while (!cursor.isAfterLast()) {
            String meetDay = cursor.getString(3);
            meetingDays.add(meetDay);
            int meetSH = cursor.getInt(4);
            meetingStartHour.add(Integer.toString(meetSH));
            int meetSM = cursor.getInt(5);
            String startMinuteString = Integer.toString(meetSM);
            if (Integer.parseInt(startMinuteString) == 0) startMinuteString = "00"; // Convert **:0 to **:00
            meetingStartMinute.add(startMinuteString);
            int meetEH = cursor.getInt(6);
            meetingEndHour.add(Integer.toString(meetEH));
            int meetEM = cursor.getInt(7);
            String endMinuteString = Integer.toString(meetEM);
            if (Integer.parseInt(endMinuteString) == 0) endMinuteString = "00";
            meetingEndMinute.add(endMinuteString);
            cursor.moveToNext();
        }

        ArrayList<ArrayList<String>> umbrellaList = new ArrayList<>();
        umbrellaList.add(meetingDays);
        umbrellaList.add(meetingStartHour);
        umbrellaList.add(meetingStartMinute);
        umbrellaList.add(meetingEndHour);
        umbrellaList.add(meetingEndMinute);

        cursor.close();
        db.close();
        return umbrellaList;
    }

    // Returns a list of students in a course, represented by the students' emails (the student PK)
    public ArrayList<String> getStudents(String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM students WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[]{course, semester, userEmail});

        if (cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();

        ArrayList<String> students = new ArrayList<>();

        while (!cursor.isAfterLast()) {
            String studentEmail = cursor.getString(0);

            students.add(studentEmail);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return students;
    }

    // Returns an ArrayList of String arrays, each array containing
    // fName, lName, studentEmail, date
    public ArrayList<String[]> getDatesPresent(String studentEmail, String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM datesPresent" +
                        " WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[]{studentEmail, course, semester, userEmail});
        if (cursor.getCount() == 0) return null;

        ArrayList<String[]> datesPresent = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String sEmail = cursor.getString(0);
            String studentName = getStudentName(sEmail, course, semester, userEmail);
            String[] nameTokens = studentName.split(" ");
            String fName = nameTokens[0];
            String lName = nameTokens[1];
            String date = cursor.getString(4);
            String[] emailAndDate = new String[]{fName, lName, sEmail, date};

            datesPresent.add(emailAndDate);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return datesPresent;
    }

    //Returns all the dates that have had at least one student marked present that day
    public ArrayList<String> getPastCourseDates(String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT recordedDate FROM datesPresent" +
                            " WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                            new String[] {course, semester, userEmail});
        if (cursor.getCount() == 0) return null;

        ArrayList<String> pastDates = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String date = cursor.getString(0);
            pastDates.add(date);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return pastDates;
    }

    // Returns a list of students that were present on the given date
    // Each student is represented as an array [lastName, firstName, email]
    public ArrayList<String[]> getStudentsPresentOnDate(String date, String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT st.lName, st.fName, dp.studentEmail " +
                                        "FROM datesPresent dp " +
                                        "INNER JOIN students st ON dp.studentEmail = st.studentEmail " +
                                        "WHERE dp.recordedDate = ? AND dp.courseName = ? AND dp.semesterName = ? AND dp.lecturerEmail = ?",
                                        new String[] {date, course, semester, userEmail});
        if (cursor.getCount() == 0) return null;

        ArrayList<String[]> studentsPresent = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String studentLast = cursor.getString(0);
            String studentFirst = cursor.getString(1);
            String studentEmail = cursor.getString(2);
            String[] studentInformation = new String[] {studentLast, studentFirst, studentEmail};

            studentsPresent.add(studentInformation);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return studentsPresent;
    }

    // Returns a list of students that were absent on the given date
    // Each student is represented as an array [lastName, firstName, email]
    public ArrayList<String[]> getStudentsAbsentOnDate(String date, String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT st.lName, st.fName, st.studentEmail " +
                                    "FROM students st " +
                                    "WHERE st.studentEmail NOT IN " +
                                        "(SELECT dp.studentEmail FROM datesPresent dp WHERE dp.recordedDate = ? " +
                                            "AND dp.courseName = ? AND dp.semesterName = ? AND dp.lecturerEmail = ?) " +
                                    "AND st.courseName = ? AND st.semesterName = ? AND st.lecturerEmail = ?",
                                        new String[] {date, course, semester, userEmail, course, semester, userEmail});
        if (cursor.getCount() == 0) return null;

        ArrayList<String[]> studentsAbsent = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String studentLast = cursor.getString(0);
            String studentFirst = cursor.getString(1);
            String studentEmail = cursor.getString(2);
            String[] studentInformation = new String[] {studentLast, studentFirst, studentEmail};

            studentsAbsent.add(studentInformation);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return studentsAbsent;
    }

    // Returns the last recorded date since a given date for a student
    // Date should be in the format yyyy-MM-dd
    // Example, if Student's dates present are 2022-10-18 (October 18, 2022) and 2022-11-01 (November 1, 2022)
    //      and the date used is 2022-10-31, this method will return 2022-10-18
    public String getLastRecordedDateSinceDate(String studentEmail, String course, String semester, String lecturerEmail, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT recordedDate FROM datesPresent " +
                        "WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ? " +
                        "AND recordedDate <= ?" +
                        "ORDER BY recordedDate DESC " +
                        "LIMIT 1",
                        new String[] {studentEmail, course, semester, lecturerEmail, date});
        if (cursor.getCount() == 0) return null;

        cursor.moveToFirst();
        String studentsLastDatePresentSinceDate = cursor.getString(0);

        cursor.close();
        db.close();
        return studentsLastDatePresentSinceDate;
    }

    // Returns the last date the given student has been present for the class. Uses the current date
    public String getLastRecordedDate(String studentEmail, String course, String semester, String lecturerEmail) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT recordedDate FROM datesPresent " +
                        "WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ? " +
                        "AND recordedDate <= ?" +
                        "ORDER BY recordedDate DESC " +
                        "LIMIT 1",
                new String[] {studentEmail, course, semester, lecturerEmail, currentDate});
        if (cursor.getCount() == 0) return null;

        cursor.moveToFirst();
        String studentsLastDatePresent = cursor.getString(0);

        cursor.close();
        db.close();
        return studentsLastDatePresent;
    }

    // Returns the name of the student associated with the email argument
    public String getStudentName(String studentEmail, String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM students WHERE studentEmail = ? " +
                        "AND courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                        new String[] {studentEmail, course, semester, userEmail});

        if (cursor.getCount() == 0) return null;
        cursor.moveToFirst();
        String studentF = cursor.getString(4);
        String studentL = cursor.getString(5);
        String studentName = studentF + " " + studentL;

        cursor.close();
        db.close();
        return studentName;
    }

    // Returns a list of the names of students that are in a class
    public ArrayList<String> getAllStudentNames(String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM students WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {course, semester, userEmail});

        if (cursor.getCount() == 0) { return null; }
        cursor.moveToFirst();

        ArrayList<String> studentNames = new ArrayList<>();

        while (!cursor.isAfterLast()) {
            String studentF = cursor.getString(4);
            String studentL = cursor.getString(5);
            String studentName = studentF + " " + studentL;
            studentNames.add(studentName);
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        return studentNames;
    }

    //
    /*
    UPDATE METHODS SECTION
     */
    //

    // Updates the semester name to newSemesterName, identifying the semester by userEmail and oldSemesterName
    public void updateSemesterName(String oldSemesterName, String newSemesterName, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE semesters SET semesterName = ? WHERE semesterName = ? AND lecturerEmail = ?",
                        new String[] {newSemesterName, oldSemesterName, userEmail});

        db.close();
    }

    // Updates the course name to newCourseName, identifying the course by userEmail and semesterName
    // and userEmail
    public void updateCourseName(String oldCourseName, String newCourseName, String semesterName, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE courses SET courseName = ? WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {newCourseName, oldCourseName, semesterName, userEmail});

        db.close();
    }

    // Updates the student's name to newSemesterName, identifying the student
    // by course, semester, userEmail, and the student's email
    public void updateStudentName(String newStudentName, String course, String semester, String userEmail, String studentEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] tokens = newStudentName.split(" ");
        db.execSQL("UPDATE students SET fName = ? WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {tokens[0], studentEmail, course, semester, userEmail});
        db.execSQL("UPDATE students SET lName = ? WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {tokens[1], studentEmail, course, semester, userEmail});

        db.close();
    }

    // Update the student's email to newEmail, identified by
    // oldEmail, course, semester, and userEmail
    public void updateStudentEmail(String oldEmail, String newEmail, String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE students SET studentEmail = ? WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {newEmail, oldEmail, course, semester, userEmail});

        db.close();
    }

    //
    /*
    DELETE METHODS SECTION
     */
    //

    public void deleteSemester(String semesterName, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM semesters WHERE semesterName = ? AND lecturerEmail = ?",
                        new String[] {semesterName, userEmail});

        db.close();
    }

    public void deleteCourse(String courseName, String semesterName, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM courses WHERE courseName = ? AND semesterName = ? AND lecturerEmail =?",
                new String[] {courseName, semesterName, userEmail});

        db.close();
    }


    public void deleteStudent(String studentEmail, String course, String semester, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM students WHERE studentEmail = ? AND courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {studentEmail, course, semester, userEmail});

        db.close();
    }

    //
    /*
    COUNT METHODS SECTION
     */
    //

    // Returns the number of students in the students table
    public long getStudentRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, "students");
        db.close();
        return count;
    }

    // Returns the number of courses in the courses table
    public long getCourseRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, "courses");
        db.close();
        return count;
    }

    // Returns the number of semesters in the semesters table
    public long getSemesterRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, "semesters");
        db.close();
        return count;
    }

    // Returns the number of meetings a course has in a week
    public long getCourseMeetingsCount(String courseName, String semesterName, String lecturerEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM meetings " +
                        "WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                        new String[] {courseName, semesterName, lecturerEmail});
        if (cursor.getCount() == 0) return 0;
        cursor.moveToFirst();
        long count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    // Returns the number of students in a class
    public long getNumberOfStudentsInClass(String courseName, String semesterName, String lecturerEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM students " +
                "WHERE courseName = ? AND semesterName = ? AND lecturerEmail = ?",
                new String[] {courseName, semesterName, lecturerEmail});
        if (cursor.getCount() == 0) return 0;
        cursor.moveToFirst();
        long count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

}