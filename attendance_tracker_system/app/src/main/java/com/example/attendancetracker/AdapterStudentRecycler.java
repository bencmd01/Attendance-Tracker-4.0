package com.example.attendancetracker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AdapterStudentRecycler extends RecyclerView.Adapter<AdapterStudentRecycler.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView studentEmail;
        public Button editButton;

        public ViewHolder (View itemView) {
            super(itemView);

            studentEmail = (TextView) itemView.findViewById(R.id.studentListName);
            editButton = (Button) itemView.findViewById(R.id.studentListEdit);
        }
    }

    private final Context context;
    private final String userEmail;
    private final String semesterName;
    private final String courseName;
    private final DBHandler db;
    private final ArrayList<String> students;

    public AdapterStudentRecycler(Context context, String userEmail, String semesterName, String courseName) {
        this.context = context;
        this.userEmail = userEmail;
        this.semesterName = semesterName;
        this.courseName = courseName;
        db = new DBHandler(context);

        students = db.getStudents(courseName, semesterName, userEmail);



    }

    @NonNull
    @Override
    public AdapterStudentRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {






        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View studentView = inflater.inflate(R.layout.layout_student_list, parent, false);

        return new ViewHolder(studentView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String studentEmail = students.get(position);
        String studentName = db.getStudentName(studentEmail, courseName, semesterName, userEmail);
        TextView studentTextView = holder.studentEmail;
        if (!studentName.equals("null null")) {
            studentTextView.setText(studentName);
        } else {
            studentTextView.setText(studentEmail);
        }
        studentTextView.setOnClickListener(view -> launchStudentDetailActivity(context, studentEmail, courseName, semesterName, userEmail));

        Button button = holder.editButton;
        button.setOnClickListener(view -> {
            Dialog studentEditDialog = new Dialog(context);
            ConstraintLayout layout = (ConstraintLayout) View.inflate(context, R.layout.dialog_student_edit, null);
            studentEditDialog.setContentView(layout);
            studentEditDialog.show();

            EditText editStudentName = (EditText) studentEditDialog.findViewById(R.id.studentEditNameEdit);
            EditText editStudentEmail = (EditText) studentEditDialog.findViewById(R.id.studentEditEmailEdit);
            Button acceptBtn = (Button) studentEditDialog.findViewById(R.id.studentEditAcceptButton);
            acceptBtn.setOnClickListener(view1 -> {
                String newStudentName = editStudentName.getText().toString().trim();
                String newStudentEmail = editStudentEmail.getText().toString().trim();

                if (newStudentName.isEmpty() && newStudentEmail.isEmpty()) {
                    studentEditDialog.dismiss();
                } else if (!newStudentName.isEmpty() && newStudentEmail.isEmpty()) {
                    db.updateStudentName(newStudentName, courseName, semesterName, userEmail, studentEmail);
                    studentTextView.setText(newStudentName);
                    studentEditDialog.dismiss();
                } else if (newStudentName.isEmpty()) {
                    db.updateStudentEmail(studentEmail, newStudentEmail, courseName, semesterName, userEmail);
                    students.set(position, newStudentEmail);
                    studentEditDialog.dismiss();
                } else {
                    db.updateStudentEmail(studentEmail, newStudentEmail, courseName, semesterName, userEmail);
                    students.set(position, newStudentEmail);
                    db.updateStudentName(newStudentName, courseName, semesterName, userEmail, newStudentEmail);
                    studentTextView.setText(newStudentName);
                    studentEditDialog.dismiss();
                }
                notifyItemChanged(position);
            });
            Button deleteBtn = (Button) studentEditDialog.findViewById(R.id.studentEditDeleteButton);
            deleteBtn.setOnClickListener(view1 -> {
                db.deleteStudent(studentEmail, courseName, semesterName, userEmail);
                students.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position,(int) db.getStudentRowCount());
                studentEditDialog.dismiss();
            });
            Button cancelBtn = (Button) studentEditDialog.findViewById(R.id.studentEditCancelButton);
            cancelBtn.setOnClickListener(view1 -> studentEditDialog.dismiss());
        });
    }

    @Override
    public int getItemCount() {
        if (students == null) {
            return 0;
        }
        return students.size();}

    private void launchStudentDetailActivity(Context context, String studentEmail, String courseName, String semesterName, String userEmail) {
        Intent intent = new Intent(context, StudentDetailsActivity.class);
        intent.putExtra("studentEmail", studentEmail);
        intent.putExtra("course", courseName);
        intent.putExtra("semester", semesterName);
        intent.putExtra("userEmail", userEmail);
        context.startActivity(intent);
    }
}