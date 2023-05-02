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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterSemesterRecycler extends RecyclerView.Adapter<AdapterSemesterRecycler.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView semesterName;
        public Button editButton;
        public RecyclerView childRV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            semesterName = (TextView) itemView.findViewById(R.id.semesterListName);
            editButton = (Button) itemView.findViewById(R.id.semesterListButton);
            childRV = (RecyclerView) itemView.findViewById(R.id.semesterCourseChildRecycler);
        }
    }

    private final Context context;
    private final List<String> semesters;
    private final String userEmail;
    private final DBHandler db;

    public AdapterSemesterRecycler(Context context, String userEmail, List<String> semesters) {
        this.context = context;
        this.semesters = semesters;
        this.userEmail = userEmail;
        db = new DBHandler(context);
    }

    @NonNull
    @Override
    public AdapterSemesterRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate our custom row layout
        View semesterView = inflater.inflate(R.layout.layout_semester_list, parent, false);

        return new ViewHolder(semesterView);
    }

    //Populate data into item
    @Override
    public void onBindViewHolder(@NonNull AdapterSemesterRecycler.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String semester = semesters.get(position);

        //Set item views based on data (just strings in this case)
        TextView semesterTextView = holder.semesterName;
        semesterTextView.setText(semester);
        semesterTextView.setOnClickListener(view -> launchCourseActivity(context, semester, userEmail));

        RecyclerView recyclerView = holder.childRV;
        AdapterCourseRecycler courseAdapter = new AdapterCourseRecycler(context, userEmail, semester);
        recyclerView.setAdapter(courseAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        Button editButton = holder.editButton;
        editButton.setOnClickListener(view -> {
            Dialog editDialogBuilder = new Dialog(context);
            ConstraintLayout layout = (ConstraintLayout) View.inflate(context, R.layout.dialog_semester_edit, null);
            editDialogBuilder.setContentView(layout);
            editDialogBuilder.show();

            EditText editSemesterName = (EditText) editDialogBuilder.findViewById(R.id.semesterEditName);
            Button acceptBtn = (Button) editDialogBuilder.findViewById(R.id.semesterEditAcceptBtn);
            acceptBtn.setOnClickListener(view1 -> {
                String newSemesterName = editSemesterName.getText().toString().trim();
                if (newSemesterName.isEmpty()) {
                    Toast.makeText(view1.getContext(), "New semester name cannot be empty", Toast.LENGTH_LONG).show();
                } else {
                    semesterTextView.setText(newSemesterName);
                    semesters.set(position, newSemesterName);
                    db.updateSemesterName(semester, newSemesterName, userEmail);
                    notifyItemChanged(position);
                    editDialogBuilder.dismiss();
                }
            });
            Button deleteBtn = (Button) editDialogBuilder.findViewById(R.id.semesterEditDeleteBtn);
            deleteBtn.setOnClickListener(view1 -> {
                db.deleteSemester(semesters.get(position), userEmail);
                semesters.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, (int) db.getSemesterRowCount());
                editDialogBuilder.dismiss();
            });
            Button cancelBtn = (Button) editDialogBuilder.findViewById(R.id.semesterEditCancelBtn);
            cancelBtn.setOnClickListener(view1 -> editDialogBuilder.dismiss());

        });
    }

    @Override
    public int getItemCount() {
        if (semesters == null) {
            return 0;
        }
        return semesters.size();
    }

    private void launchCourseActivity(Context context, String semester, String userEmail) {
        Intent intent = new Intent(context, CourseActivity.class);
        intent.putExtra("semester", semester);
        intent.putExtra("userEmail", userEmail);
        context.startActivity(intent);
    }
}
