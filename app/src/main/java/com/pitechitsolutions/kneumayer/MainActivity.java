package com.pitechitsolutions.kneumayer;
import android.Manifest;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private EditText etPin;
    private TextView tvDateTime;
    private GridLayout keypadGrid;
    private Button btnAdminActivities;
    private ImageView imgReports, imgHistory;
    private String userPin;  // Already defined, but ensure it is managed correctly.
    private long userId;     // Define this to manage user ID globally within the class.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDateTimeDisplay();
        setupKeypad();
        setupButtonListeners();
    }

    private void initViews() {
        etPin = findViewById(R.id.etPin);
        tvDateTime = findViewById(R.id.tvDateTime);
        keypadGrid = findViewById(R.id.gridLayout);
        btnAdminActivities = findViewById(R.id.btnAdminActivities);

        imgReports = findViewById(R.id.imgReports);
        imgHistory = findViewById(R.id.imgHistory);

        // Ensure they all resolve correctly:
        if (etPin == null || tvDateTime == null || keypadGrid == null || btnAdminActivities == null ||
                imgReports == null || imgHistory == null) {
            Log.e("MainActivity", "One or more UI components failed to initialize.");
            Toast.makeText(this, "UI initialization error. Please restart the app.", Toast.LENGTH_SHORT).show();
            finish();  // Safely close the app to avoid undefined behavior
        }
    }


    private void setupDateTimeDisplay() {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        tvDateTime.setText(sdf.format(new Date()));
        // Update the TextView every second to show real-time
        final Runnable updater = new Runnable() {
            @Override
            public void run() {
                tvDateTime.setText(sdf.format(new Date()));
                tvDateTime.postDelayed(this, 1000);
            }
        };
        tvDateTime.post(updater);
    }

    private void setupKeypad() {
        // Create keypad buttons dynamically
        // Assume 0-9 buttons plus a delete button
        for (int i = 0; i < 10; i++) {
            Button button = new Button(this);
            button.setText(String.valueOf(i));
            button.setOnClickListener(v -> {
                etPin.setText(etPin.getText().toString() + ((Button) v).getText().toString());
            });
            keypadGrid.addView(button);
        }
        Button delButton = new Button(this);
        delButton.setText("DEL");
        delButton.setOnClickListener(v -> {
            String text = etPin.getText().toString();
            if (!text.isEmpty()) {
                etPin.setText(text.substring(0, text.length() - 1));
            }
        });
        keypadGrid.addView(delButton);

        // Adding an Enter button
        Button enterButton = new Button(this);
        enterButton.setText("ENTER");
        enterButton.setOnClickListener(v -> retrieveAndLogUserDetails());
        keypadGrid.addView(enterButton);
    }

    private void retrieveAndLogUserDetails() {
        userPin = etPin.getText().toString();  // Retrieve the user PIN
        Cursor cursor = DatabaseHelper.getInstance(this).getUserDetails(userPin);
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndex("id"));  // Retrieve user ID
            String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.NAME));
            String surname = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SURNAME));
            String employeeNumber = cursor.getString(cursor.getColumnIndex(DatabaseHelper.EMPLOYEE_NUMBER));

            Log.d("MainActivity", "User Details - Name: " + name + ", Surname: " + surname + ", Employee Number: " + employeeNumber);

            boolean isClockedIn = DatabaseHelper.getInstance(this).isUserClockedIn(userPin);
            if (!isClockedIn) {
                checkPermissionsAndLaunchCamera();  // Use this method to handle permissions and launch camera
            } else {
                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                DatabaseHelper.getInstance(this).recordClockOut(userPin, currentTime);
                showClockInOutDialog("Clocked Out: " + currentTime);
            }
            cursor.close();
        } else {
            Log.d("MainActivity", "No user found with the entered PIN.");
            if (cursor != null) cursor.close();
        }
    }


    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void checkPermissionsAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_IMAGE_CAPTURE);
        } else {
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera and storage permissions are required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Fetch userId again or pass it through the intent when launching the camera
            handleCapturedImage(imageBitmap, userId);  // Adjust accordingly
        }
    }

    private String saveImage(Bitmap imageBitmap, String fileName) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, fileName);
        try (FileOutputStream out = new FileOutputStream(image)) {
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (IOException e) {
            Log.e("MainActivity", "Error saving image", e);
        }
        return image.getAbsolutePath();
    }

    private void handleCapturedImage(Bitmap imageBitmap, long userId) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "USER_" + this.userId + "_" + userPin + "_" + timeStamp + ".png";
        String filePath = saveImage(imageBitmap, fileName);

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        if (DatabaseHelper.getInstance(this).recordClockIn(userPin, currentTime, filePath)) {
            showClockInOutDialog("Clocked In: " + currentTime);
        } else {
            showClockInOutDialog("Failed to clock in");
        }
    }


    private void showClockInOutDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Clock Status")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }


    private void setupButtonListeners() {
        btnAdminActivities.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserManagementActivity.class);
            startActivity(intent);
        });


        imgReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });

        imgHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmployeeSchedulehistoryActivity.class);
            startActivity(intent);
        });
    }
}