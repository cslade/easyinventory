package com.kinvo.easyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private EditText etApiKey, etApiSecret, etLocationId;
    private Button btnAuthenticate, btnGoToSearch;
    private String authToken; // Store authentication token
    private int locationId; // Store Location ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiKey = findViewById(R.id.etApiKey);
        etApiSecret = findViewById(R.id.etApiSecret);
        etLocationId = findViewById(R.id.etLocationId);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);
        btnGoToSearch = findViewById(R.id.btnSearchProduct);

        btnAuthenticate.setOnClickListener(view -> authenticate());

        btnGoToSearch.setOnClickListener(view -> {
            if (authToken == null || authToken.isEmpty()) {
                Toast.makeText(MainActivity.this, "Authenticate first!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(MainActivity.this, ProductSearchActivity.class);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("LOCATION_ID", locationId);
                startActivity(intent);
            }
        });
    }

    private void authenticate() {
        String apiKey = etApiKey.getText().toString().trim();
        String apiSecret = etApiSecret.getText().toString().trim();

        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            Toast.makeText(this, "Enter API Key and Secret!", Toast.LENGTH_SHORT).show();
            return;
        }

        String credentials = apiKey + ":" + apiSecret;
        authToken = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        Log.d("AuthDebug", "Encoded Auth Token: " + authToken); // Debugging

        String url = "https://api.eposnowhq.com/api/v4/product/1";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("LocationId")) {
                            locationId = response.getInt("LocationId");
                        }

                        Toast.makeText(this, "Authentication Successful!", Toast.LENGTH_SHORT).show();
                        Log.d("AuthDebug", "Authentication successful, Location ID: " + locationId);

                        // Enable button to go to ProductSearchActivity
                        btnGoToSearch.setEnabled(true);
                    } catch (JSONException e) {
                        Log.e("AuthError", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(this, "Error processing authentication", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("AuthError", "Authentication failed: " + error.getMessage());
                    Toast.makeText(this, "Authentication failed!", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + authToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}

