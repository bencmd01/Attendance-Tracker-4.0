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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AdapterCourseRecycler extends RecyclerView.Adapter<AdapterCourseRecycler.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView courseName;
        public ImageButton editButton;

        public ViewHolder(View itemView) {
            super(itemView);

            courseName = (TextView) itemView.findViewById(R.id.courseListName);
            editButton = (ImageButton) itemView.findViewById(R.id.courseListBtn);
        }
    }

    private final Context context;
    private final String userEmail;
    private final String semesterName;
    private final DBHandler db;
    private final ArrayList<String> courses;

    public AdapterCourseRecycler(Context context, String userEmail, String semesterName) {
        this.context = context;
        this.userEmail = userEmail;
        this.semesterName = semesterName;
        db = new DBHandler(context);

        courses = db.getCourses(semesterName, userEmail);
    }

    @NonNull
    @Override
    public AdapterCourseRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View courseView = inflater.inflate(R.layout.layout_course_list, parent, false);

        return new ViewHolder(courseView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String course = courses.get(position);

        TextView courseTextView = holder.courseName;
        courseTextView.setText(course);
        courseTextView.setOnClickListener(view -> launchStudentsActivity(context, course, semesterName, userEmail));

        ImageButton button = holder.editButton;
        button.setOnClickListener(view -> {
            Dialog editDialogBuilder = new Dialog(context);
            ConstraintLayout layout = (ConstraintLayout) View.inflate(context, R.layout.dialog_course_edit, null);
            editDialogBuilder.setContentView(layout);
            editDialogBuilder.show();

            EditText editCourseName = (EditText) editDialogBuilder.findViewById(R.id.courseEditName);
            Button acceptBtn = (Button) editDialogBuilder.findViewById(R.id.courseEditAcceptBtn);
            acceptBtn.setOnClickListener(view1 -> {
                String newCourseName = editCourseName.getText().toString().trim();
                if (newCourseName.isEmpty()) {
                    Toast.makeText(view1.getContext(), "Course name cannot be empty", Toast.LENGTH_LONG).show();
                } else {
                    courseTextView.setText(newCourseName);
                    courses.set(position, newCourseName);
                    db.updateCourseName(course, newCourseName, semesterName, userEmail);
                    notifyItemChanged(position);
                    editDialogBuilder.dismiss();
                }
            });

            Button deleteBtn = (Button) editDialogBuilder.findViewById(R.id.courseEditDeleteBtn);
            deleteBtn.setOnClickListener(view1 -> {
                db.deleteCourse(courses.get(position), semesterName, userEmail);
                courses.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, (int) db.getCourseRowCount());
                editDialogBuilder.dismiss();
            });

            Button cancelBtn = (Button) editDialogBuilder.findViewById(R.id.courseEditCancelBtn);
            cancelBtn.setOnClickListener(view1 -> editDialogBuilder.dismiss());
        });
    }

    @Override
    public int getItemCount() {
        if (courses == null) {
            return 0;
        }
        return courses.size();
    }

    private void launchStudentsActivity(Context context, String course, String semester, String userEmail) {
        Intent intent = new Intent(context, SectionViewActivity.class);
        intent.putExtra("course", course);
        intent.putExtra("semester", semester);
        intent.putExtra("userEmail", userEmail);
        context.startActivity(intent);
    }
}
