package com.aldenocain.geolookup;

import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Geolookup extends AppCompatActivity {
    SQLiteCRUD dbHelper;
    Geocoder geocoder;
    Spinner locationSpinner;
    EditText addressEditText, latitudeEditText, longitudeEditText;
    Button addButton, deleteButton, updateButton, clearButton, openMapsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geolookup);
        geocoder = new Geocoder(this);
        dbHelper = new SQLiteCRUD(this);
        locationSpinner = findViewById(R.id.locationSpinner);
        addressEditText = findViewById(R.id.addressEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        addButton = findViewById(R.id.addButton);
        deleteButton = findViewById(R.id.deleteButton);
        updateButton = findViewById(R.id.updateButton);
        clearButton = findViewById(R.id.clearButton);
        openMapsButton = findViewById(R.id.openMaps);
        readAndGeocodeInputFile();
        populateLocationSpinner();
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedLocation = locationSpinner.getSelectedItem().toString();
                int selectedID = extractIDFromSelectedItem(selectedLocation);

                // Split the selected location string to extract address, latitude, and longitude
                String[] parts = selectedLocation.split(": ");
                if (parts.length == 2) {
                    String[] latLong = parts[1].split(", ");
                    if (latLong.length == 2) {
                        String address = dbHelper.read(selectedID);
                        String latitude = latLong[0];
                        String longitude = latLong[1];

                        // Set the values in the EditText fields
                        addressEditText.setText(address);
                        latitudeEditText.setText(latitude);
                        longitudeEditText.setText(longitude);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                Toast.makeText(Geolookup.this, "Please select one of the entries from the dropdown menu..", Toast.LENGTH_LONG).show();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve values from the latitude and longitude text fields
                double latitude = Double.parseDouble(latitudeEditText.getText().toString());
                double longitude = Double.parseDouble(longitudeEditText.getText().toString());

                // Perform geocoding to get the address
                String address = performGeocoding(latitude, longitude);

                // Get the next available ID
                int nextID = dbHelper.getNextAvailableID();

                // Create a new record in the database
                dbHelper.create(nextID, address, latitude, longitude);

                // Update the spinner
                populateLocationSpinner();

                // Clear the text fields
                clearTextFields();

                // Check if the nextID is within the bounds of the adapter
                int lastPosition = locationSpinner.getAdapter().getCount() - 1;
                if (lastPosition >= 0) {
                    // Select the last record (the one you just added) in the spinner
                    locationSpinner.setSelection(lastPosition);
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected item from the spinner
                String selectedItem = locationSpinner.getSelectedItem().toString();

                // Extract the ID from the selected item (you may need to parse it)
                int selectedID = extractIDFromSelectedItem(selectedItem);

                if (selectedID != -1) {
                    // Delete the record with the selected ID
                    dbHelper.delete(selectedID);

                    // Update the spinner and clear the text fields (if needed)
                    populateLocationSpinner();
                    clearTextFields();
                } else {
                    Toast.makeText(Geolookup.this, "No record selected.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected item from the Spinner
                String selectedLocation = locationSpinner.getSelectedItem().toString();
                int selectedID = extractIDFromSelectedItem(selectedLocation);

                if (selectedID != -1) {
                    // Retrieve the new values from EditText fields
                    String newAddress = addressEditText.getText().toString();
                    double newLatitude = Double.parseDouble(latitudeEditText.getText().toString());
                    double newLongitude = Double.parseDouble(longitudeEditText.getText().toString());

                    // Update the record in the database
                    dbHelper.update(selectedID, newAddress, newLatitude, newLongitude);

                    // Update the item in the Spinner
                    String updatedItem = selectedID + ": " + newLatitude + ", " + newLongitude;
                    locationSpinner.setSelection(locationSpinner.getSelectedItemPosition()); // Refresh the Spinner
                    Toast.makeText(Geolookup.this, "Record updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Geolookup.this, "Invalid selection", Toast.LENGTH_SHORT).show();
                }
            }
        });


        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearTextFields();
            }
        });
        openMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve the latitude and longitude values from the EditText fields
                String latitudeStr = latitudeEditText.getText().toString();
                String longitudeStr = longitudeEditText.getText().toString();

                double latitude = Double.parseDouble(latitudeStr);
                double longitude = Double.parseDouble(longitudeStr);

                // Create an Intent to open Google Maps with the specified coordinates
                Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps"); // Use Google Maps app
                startActivity(mapIntent);
            }
        });

    }

    private int extractIDFromSelectedItem(String selectedItem) {
        try {
            // Split the selected item by ":" to get the ID part
            String[] parts = selectedItem.split(":");
            if (parts.length >= 1) {
                // Trim any extra spaces and parse the ID as an integer
                String idString = parts[0].trim();
                return Integer.parseInt(idString);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 to indicate an error or no valid ID found
    }

    private void clearTextFields() {
        EditText addressEditText = findViewById(R.id.addressEditText);
        EditText latitudeEditText = findViewById(R.id.latitudeEditText);
        EditText longitudeEditText = findViewById(R.id.longitudeEditText);

        addressEditText.setText("");
        latitudeEditText.setText("");
        longitudeEditText.setText("");
    }

    private String performGeocoding(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                return addresses.get(0).getAddressLine(0);
            } else {
                return "No address found for the given coordinates.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Geocoding error";
        }
    }

    private void readAndGeocodeInputFile() {
        try {
            // Create a new SQLiteCRUD instance
            SQLiteCRUD dbHelper = new SQLiteCRUD(this);

            // Clear and initialize the table
            dbHelper.clearTable();
            dbHelper.initTable();

            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.latlongs)));
            String line;
            int nextID = dbHelper.getNextAvailableID();

            while ((line = reader.readLine()) != null) {
                // Split the line into latitude and longitude
                String[] latLong = line.split(",");
                if (latLong.length == 2) {
                    double latitude = Double.parseDouble(latLong[0].trim());
                    double longitude = Double.parseDouble(latLong[1].trim());

                    String address = performGeocoding(latitude, longitude);

                    // Add a new location record
                    dbHelper.create(nextID, address, latitude, longitude);

                    nextID++; // Increment the ID for the next record
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateLocationSpinner() {
        // Get a cursor for all location records from the database
        Cursor cursor = dbHelper.getAlllocations();

        // Create an array to store location strings in the format "id: latitude, longitude"
        List<String> locationStrings = new ArrayList<>();

        // Check if the cursor is valid and contains data
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                locationStrings.add(id + ": " + latitude + ", " + longitude);
            } while (cursor.moveToNext());

            // Close the cursor when done
            cursor.close();
        }

        // Create an ArrayAdapter and set it to the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
    }

}
