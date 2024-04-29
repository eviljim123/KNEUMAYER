package com.pitechitsolutions.kneumayer;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class UserManagementActivity extends Activity {
    private EditText etName, etSurname, etEmployeeNumber, etUserPin;
    private Button btnSave, btnDelete;
    private ListView lvUsers;
    private DatabaseHelper dbHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        dbHelper = DatabaseHelper.getInstance(this);
        initializeUI();
        setupButtons();
        populateUserList();
    }

    private void initializeUI() {
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmployeeNumber = findViewById(R.id.etEmployeeNumber);
        etUserPin = findViewById(R.id.etUserPin);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        lvUsers = findViewById(R.id.lvUsers);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveUser());
        btnDelete.setOnClickListener(v -> deleteUser());
        lvUsers.setOnItemClickListener((adapterView, view, position, id) -> {
            String userDetails = (String) adapterView.getItemAtPosition(position);
            loadUserDetails(userDetails);
        });
    }

    private void saveUser() {
        String name = etName.getText().toString();
        String surname = etSurname.getText().toString();
        String employeeNumber = etEmployeeNumber.getText().toString();
        String userPin = etUserPin.getText().toString();

        if (name.isEmpty() || surname.isEmpty() || employeeNumber.isEmpty() || userPin.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exists = dbHelper.validateUserPin(userPin);
        boolean isSuccess = exists ? dbHelper.editUser(name, surname, employeeNumber, userPin) : dbHelper.addUser(name, surname, employeeNumber, userPin);
        if (isSuccess) {
            Toast.makeText(this, exists ? "User updated successfully" : "User added successfully", Toast.LENGTH_SHORT).show();
            populateUserList();
        } else {
            Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteUser() {
        String userPin = etUserPin.getText().toString();
        if (userPin.isEmpty()) {
            Toast.makeText(this, "User PIN is required to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isSuccess = dbHelper.deleteUser(userPin);
        if (isSuccess) {
            Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
            populateUserList();
        } else {
            Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserDetails(String userDetails) {
        String userPin = extractUserPinFromUserDetails(userDetails);
        Cursor cursor = dbHelper.getUserDetails(userPin);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.NAME));
            String surname = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SURNAME));
            String employeeNumber = cursor.getString(cursor.getColumnIndex(DatabaseHelper.EMPLOYEE_NUMBER));
            etName.setText(name);
            etSurname.setText(surname);
            etEmployeeNumber.setText(employeeNumber);
            etUserPin.setText(userPin);
            cursor.close();
        } else {
            Toast.makeText(this, "User details not found.", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
        }
    }



    private String extractUserPinFromUserDetails(String userDetails) {
        String[] parts = userDetails.trim().split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : ""; // Return empty if extraction fails
    }

    private void populateUserList() {
        ArrayList<String> userList = new ArrayList<>();
        Cursor cursor = dbHelper.getAllUsers();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.NAME));
            String surname = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SURNAME));
            String userPin = cursor.getString(cursor.getColumnIndex(DatabaseHelper.USER_PIN));
            userList.add(name + " " + surname + " " + userPin);
        }
        cursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        lvUsers.setAdapter(adapter);
    }
}
