package com.example.attendancetracker;

import android.util.Log;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**     Semester: Fall 2022
 * This class holds the algorithm for sending emails to the user (lecturer) or students.
 *
 * Constructor
 *      Sets the session properties required to send an email.
 * emailStudentQRCode
 *      Constructs a message to the student and sends the specified file (QR code) to
 *      the specified student email.
 * emailInstructorReports
 *      Constructs a message to the instructor and sends the specified files (CSV files)
 *      to the specified lecturer email.
 */
public class EmailSender extends javax.mail.Authenticator {

    private static final String TAG = "DEBUG";

    // attendance.tracker.pfw@gmail.com for gmail
    // attendance.tracker.pfw@outlook.com for outlook (microsoft)
    private static final String USER = "attendance.tracker.pfw@outlook.com";

    // The following field does not work for authentication, as "less secure app access"
    // is no longer available (also the same password for outlook account)
    // private final String PASSWORD = "PurdueFortWayneAttendanceTracker123";

    // xwukgmlfgibevfde for gmail app password
    // hpoaviagzwckefbz for outlook app password
    private static final String APP_PASSWORD = "hpoaviagzwckefbz";

    // Possible errors with
    // smtp.gmail.com for gmail
    // smtp.office365.com for outlook/microsoft
    private static final String HOST = "smtp.office365.com";

    // Possible Gmail Ports:
    //     25, 465, 587
    // Possible Microsoft Exchange Ports:
    //     25, 587
    private static final int DESTINATION_PORT = 587;

    private final Session session;

    public EmailSender() {

        // Time the email will try to connect before showing an error
        final String TIMEOUT = "10000";

        // Print IP addresses of the host.
        try {
            Log.d(TAG, "Email IP: " + InetAddress.getByName(HOST));
            Log.d(TAG, "All Email IPs: " + Arrays.toString(InetAddress.getAllByName(HOST)));
        } catch (UnknownHostException e) {
            Log.e(TAG, "Email IP error: " + e.getMessage());
        }

        // Set properties for session.
        Properties props = new Properties();
        props.put("mail.smtp.user", USER);
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", String.valueOf(DESTINATION_PORT));
        props.put("mail.smtp.starttls.enable","true");
//        props.put("mail.smtp.ssl.enable","true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.debug", "true");

        // Timeout properties
        props.put("mail.smtp.connectiontimeout", TIMEOUT);
        props.put("mail.smtp.timeout", TIMEOUT);

//        props.setProperty("mail.smtp.quitwait", "false");

        // Authenticate credentials of the Attendance Tracker Email
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USER, APP_PASSWORD);
            }
        });
        session.setDebug(true);
    }

    /**
     * Constructs a message to the student and sends the specified file (QR code) to
     * the specified student email.
     */
    public synchronized void emailStudentQRCode(String sectionName, String studentName,
                                                String studentEmail, String userName, File qrCode)
    {
        try
        {
            MimeMessage email = new MimeMessage(session);

            // From, Recipient(s), and Subject
            email.setFrom(new InternetAddress(USER));
            email.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(studentEmail));
            email.setSubject(sectionName + ": Student QR Code");
            Log.d(TAG,"Student Email Address: " +
                    new InternetAddress(studentEmail).getAddress());

            // Body
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(
                    "Dear " + studentName + ",\n" +
                    "\n" +
                    "Attached is the QR Code you will use to check into class.\n" +
                    "\n" +
                    "Sincerely,\n" +
                    userName, "text/plain");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            // File added to email
            mimeBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(qrCode);
            mimeBodyPart.setDataHandler(new DataHandler(source));
            mimeBodyPart.setFileName(qrCode.getName());
            multipart.addBodyPart(mimeBodyPart);

            // Set email content
            email.setContent(multipart);

            // Send the email
            Log.d(TAG, "Email: Sending...");
            Transport transport = session.getTransport("smtp");
            transport.connect(HOST, DESTINATION_PORT, USER, APP_PASSWORD);
            transport.sendMessage(email, email.getAllRecipients());
            transport.close();
//            Transport.send(email);
            Log.d(TAG, "Email: Sent message successfully!");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Constructs a message to the instructor and sends the specified files (CSV files)
     * to the specified lecturer email.
     */
    public synchronized void emailInstructorReports(String semesterName, String sectionName,
                                                    String userEmail, String userName,
                                                    File... reportFiles)
    {
        try
        {
            MimeMessage email = new MimeMessage(session);

            // From, Recipient(s), and Subject
            email.setFrom(new InternetAddress(USER));
            email.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(userEmail));
            email.setSubject(semesterName + ", " + sectionName + " Attendance Reports");
            Log.d(TAG,"User Email Address: " + new InternetAddress(userEmail).getAddress());

            // Body
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(
                    "Dear " + userName + ",\n" +
                    "\n" +
                    "Attached are the requested attendance reports.\n" +
                    "The 1st report contains all past meeting dates and the students present and " +
                            "absent on each meeting date.\n" +
                    "The 2nd report contains the requested meeting date and the students present and " +
                            "absent on the meeting date.\n" +
                    "The 3rd report contains the last date every student the class.\n" +
                    "Dates are in the format YYYY-MM-DD.\n" +
                    "\n" +
                    "Sincerely,\n" +
                    "PFW Attendance Tracker", "text/plain");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            // Add files to email
            for (File report : reportFiles) {
                mimeBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(report);

                mimeBodyPart.setDataHandler(new DataHandler(source));
                mimeBodyPart.setFileName(report.getName());

                multipart.addBodyPart(mimeBodyPart);
            }

            // Set email content
            email.setContent(multipart);

            // Send the email
            Log.d(TAG, "Email: Sending...");
            Transport transport = session.getTransport("smtp");
            transport.connect(HOST, DESTINATION_PORT, USER, APP_PASSWORD);
            transport.sendMessage(email, email.getAllRecipients());
            transport.close();
//            Transport.send(email);
            Log.d(TAG, "Email: Sent message successfully!");

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}