package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private EditText etApiKey, etApiSecret, etLocationId;
    private Button btnLogin;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etApiKey = findViewById(R.id.etApiKey);
        etApiSecret = findViewById(R.id.etApiSecret);
        etLocationId = findViewById(R.id.etLocationId); // ✅ Added Location ID input field
        btnLogin = findViewById(R.id.btnAuthenticate);

        requestQueue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(view -> authenticateUser());
    }

    private void authenticateUser() {
        String apiKey = etApiKey.getText().toString().trim();
        String apiSecret = etApiSecret.getText().toString().trim();

        int locationId;
        try {
            locationId = Integer.parseInt(etLocationId.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Location ID!", Toast.LENGTH_SHORT).show();
            Log.e("AUTH", "❌ Location ID must be a number!");
            return;  // Exit the method if conversion fails
        }

        if (apiKey.isEmpty() || apiSecret.isEmpty() || locationId == 0) {
            Toast.makeText(this, "API Key, Secret, and Location ID are required!", Toast.LENGTH_SHORT).show();
            Log.e("AUTH", "❌ API Key, Secret, or Location ID is missing!");
            return;
        }

        // ✅ Correct Authentication URL
        String url = "https://api.eposnowhq.com/api/v4/Inventory/stocks?LocationID=" + locationId; // ✅ Use new authentication URL
        String credentials = apiKey + ":" + apiSecret;
        String encodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        Log.d("AUTH", "🔄 Starting authentication request...");
        Log.d("AUTH", "Request URL: " + url);
        Log.d("AUTH", "Encoded Credentials: " + encodedCredentials);
        Log.d("AUTH", "Using Location ID: " + locationId);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("AUTH", "✅ API Response: " + response.toString());

                        // ✅ Save Auth Token & Location ID
                        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("authToken", encodedCredentials);
                        editor.putInt("locationId", locationId);  // ✅ Store as Integer
                        editor.apply();

                        Log.d("AUTH", "✅ Auth Token & Location ID saved!");
                        Toast.makeText(this, "Authentication Successful!", Toast.LENGTH_SHORT).show();

                        // ✅ Navigate to Product Search Screen
                        Intent intent = new Intent(LoginActivity.this, ProductSearchActivity.class);
                        startActivity(intent);
                        finish();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("AUTH", "❌ JSON Parsing Error: " + e.getMessage());
                        Toast.makeText(this, "Error parsing authentication response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("AUTH_ERROR", "❌ Authentication request failed!");
                    Log.e("AUTH_ERROR", "❌ Volley Error: " + error.toString());

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = new String(error.networkResponse.data);
                        Log.e("AUTH_ERROR", "❌ Status Code: " + statusCode);
                        Log.e("AUTH_ERROR", "❌ Response Body: " + responseBody);

                        if (statusCode == 401) {
                            Toast.makeText(this, "Invalid API Key or Secret!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Authentication Failed! Check API credentials.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e("AUTH_ERROR", "❌ No network response received.");
                        Toast.makeText(this, "❌ No response from server. Check internet!", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + encodedCredentials);
                headers.put("Content-Type", "application/xml");  // ✅ EPOSNow requires XML content-type
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

}
