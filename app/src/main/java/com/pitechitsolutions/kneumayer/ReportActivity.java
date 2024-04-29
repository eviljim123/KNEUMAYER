package com.pitechitsolutions.kneumayer;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.util.Log;  // Import the Log class
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private EditText etEmailAddress;
    private ListView lvReportResults;
    private Button btnStartDate, btnEndDate, btnGenerateReport, btnFilter;
    private Calendar startDate, endDate;
    private ScheduleAdapter scheduleAdapter;
    private ArrayList<Schedule> currentSchedules;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        etEmailAddress = findViewById(R.id.etEmailAddress);
        lvReportResults = findViewById(R.id.lvReportResults);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnFilter = findViewById(R.id.filterbtn);

        initializeDatePickers();
        setupFilterButton();
        setupGenerateReportButton();
    }

    private void displayData(ArrayList<Schedule> schedules) {
        if (schedules.isEmpty()) {
            Toast.makeText(this, "No data found for selected dates.", Toast.LENGTH_SHORT).show();
        } else {
            if (scheduleAdapter == null) {
                scheduleAdapter = new ScheduleAdapter(this, schedules);
                lvReportResults.setAdapter(scheduleAdapter);
            } else {
                scheduleAdapter.clear();
                scheduleAdapter.addAll(schedules);
                scheduleAdapter.notifyDataSetChanged();
            }
        }
        currentSchedules = schedules;  // Keep a reference to the currently displayed schedules
    }

    private void generateReport() {
        DatabaseHelper db = DatabaseHelper.getInstance(getApplicationContext());
        ArrayList<Schedule> schedules = db.getSchedulesByDateRange(formatDate(startDate), formatDate(endDate));
        displayData(schedules);
    }

    private void exportAndEmailReport() {
        if (currentSchedules == null || currentSchedules.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }
        File csvFile = createCsvFile(currentSchedules);
        if (csvFile != null) {
            progressDialog = ProgressDialog.show(this, "Sending Email", "Please wait...", true);
            MailSender.sendEmailInBackground(etEmailAddress.getText().toString(), "Schedule Report", "Here is the scheduled report.", csvFile, this::handleEmailResult);
        }
    }

    private void handleEmailResult(String result) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        showResultDialog(result);
    }



    private File createCsvFile(ArrayList<Schedule> schedules) {
        File exportDir = new File(getFilesDir(), "MyAppExports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Log.e("ReportActivity", "Failed to create directory for exports");
            Toast.makeText(this, "Failed to create directory for exports", Toast.LENGTH_SHORT).show();
            return null;
        }

        String fileName = getFileName();
        File file = new File(exportDir, fileName);
        try (CSVWriter csvWrite = new CSVWriter(new FileWriter(file))) {
            String[] header = {"User Name", "Date", "Clock In Time", "Clock Out Time"};
            csvWrite.writeNext(header);
            for (Schedule schedule : schedules) {
                csvWrite.writeNext(new String[]{schedule.getUserName(), schedule.getDate(), schedule.getClockInTime(), schedule.getClockOutTime()});
            }
            Log.d("ReportActivity", "CSV file written successfully: " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            Log.e("ReportActivity", "Error writing CSV file", e);
            Toast.makeText(this, "Error writing CSV file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String getFileName() {
        SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileNameDate = fileNameDateFormat.format(new Date());
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);
        return "Generated_CSV_File_" + formattedStartDate + "_to_" + formattedEndDate + "_" + fileNameDate + ".csv";
    }


    private void sendEmail(File file) {
        Uri path = FileProvider.getUriForFile(this, "com.pitechitsolutions.kneumayer.fileprovider", file);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.dir/email");
        String[] to = {etEmailAddress.getText().toString()};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Schedule Report");
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> applyDateFilter());
    }

    private void applyDateFilter() {
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Please select start and end dates before filtering.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startDate.after(endDate)) {
            Toast.makeText(this, "Start date must be before end date.", Toast.LENGTH_LONG).show();
            return;
        }
        generateReport();
    }

    private void showResultDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupGenerateReportButton() {
        btnGenerateReport.setOnClickListener(view -> {
            if (startDate == null || endDate == null) {
                Toast.makeText(this, "Please select start and end dates before generating the report.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startDate.after(endDate)) {
                Toast.makeText(this, "Start date must be before end date.", Toast.LENGTH_LONG).show();
                return;
            }
            exportAndEmailReport();
        });
    }

    private String formatDate(Calendar date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.getTime());
    }

    private void showDatePicker(boolean isStartDate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosenDate = Calendar.getInstance();
            chosenDate.set(year, month, dayOfMonth);
            String formattedDate = formatDate(chosenDate);
            if (isStartDate) {
                startDate = chosenDate;
                btnStartDate.setText(formattedDate);
            } else {
                endDate = chosenDate;
                btnEndDate.setText(formattedDate);
            }
        }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void initializeDatePickers() {
        btnStartDate.setOnClickListener(view -> showDatePicker(true));
        btnEndDate.setOnClickListener(view -> showDatePicker(false));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MailSender.shutDownExecutor(); // Ensure all threads are terminated when the activity is destroyed
    }

}
