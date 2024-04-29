package com.pitechitsolutions.kneumayer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.AdapterView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class EmployeeSchedulehistoryActivity extends AppCompatActivity {

    private TextView tvHeader;
    private DatePicker dpDateFilter;
    private Button btnFilter;
    private ListView lvEmployeeSchedules;
    private ScheduleAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_schedulehistory);

        tvHeader = findViewById(R.id.tvHeader);
        dpDateFilter = findViewById(R.id.dpDateFilter);
        btnFilter = findViewById(R.id.btnFilter);
        lvEmployeeSchedules = findViewById(R.id.lvEmployeeSchedules);

        dbHelper = DatabaseHelper.getInstance(this);

        setupListViewAdapter();
        loadData();

        btnFilter.setOnClickListener(v -> {
            String selectedDate = getDateFromDatePicker();
            filterData(selectedDate);
        });

        lvEmployeeSchedules.setOnItemClickListener((parent, view, position, id) -> {
            Schedule schedule = adapter.getItem(position);
            if (schedule != null) {
                showDetailsDialog(schedule);
            }
        });
    }

    private void setupListViewAdapter() {
        adapter = new ScheduleAdapter(this, new ArrayList<>());
        lvEmployeeSchedules.setAdapter(adapter);
    }

    private void loadData() {
        ArrayList<Schedule> schedules = dbHelper.getAllSchedules();
        adapter.updateData(schedules);
    }

    private String getDateFromDatePicker() {
        int day = dpDateFilter.getDayOfMonth();
        int month = dpDateFilter.getMonth() + 1;
        int year = dpDateFilter.getYear();
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private void filterData(String dateFilter) {
        Log.d("Filter", "Filtering data for date: " + dateFilter);
        ArrayList<Schedule> filteredSchedules = dbHelper.getSchedulesByDate(dateFilter);
        if (filteredSchedules.isEmpty()) {
            Log.d("Filter", "No schedules found for this date");
            Toast.makeText(this, "No schedules found for this date", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("Filter", "Schedules found: " + filteredSchedules.size());
        }
        adapter.updateData(filteredSchedules);
    }

    private void showDetailsDialog(Schedule schedule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_details, null);
        builder.setView(dialogView);

        TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvClockInTime = dialogView.findViewById(R.id.tvClockInTime);
        TextView tvClockOutTime = dialogView.findViewById(R.id.tvClockOutTime);
        ImageView ivUserPhoto = dialogView.findViewById(R.id.ivUserPhoto);

        tvUserName.setText(schedule.getUserName());
        tvDate.setText(schedule.getDate());
        tvClockInTime.setText(schedule.getClockInTime());
        tvClockOutTime.setText(schedule.getClockOutTime());

        String photoPath = schedule.getPhotoPath(); // Assuming Schedule has this field
        if (photoPath != null && !photoPath.isEmpty()) {
            ivUserPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        } else {
            ivUserPhoto.setImageResource(R.drawable.ic_no_image); // Placeholder if no image available
        }

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
