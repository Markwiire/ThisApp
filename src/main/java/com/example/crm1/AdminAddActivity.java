package com.example.crm1;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminAddActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://ecdgohdfcfumamdpxtmr.supabase.co/rest/v1/users";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZGdvaGRmY2Z1bWFtZHB4dG1yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwNDMwMzAsImV4cCI6MjA2MzYxOTAzMH0.PbRiIzxtZ600b89Q2tu0VE5gnr35HWTwPZogtPPmRNg";
    private static final String PREFS_NAME = "UserPrefs";
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activityadmin);

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnAddAdmin = findViewById(R.id.btnAddAdmin);
        Button btnBack = findViewById(R.id.btnBack);

        btnAddAdmin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < MIN_PASSWORD_LENGTH) {
                Toast.makeText(this, "Пароль должен содержать минимум " + MIN_PASSWORD_LENGTH + " символов", Toast.LENGTH_SHORT).show();
                return;
            }

            checkUsernameUnique(username, password);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void checkUsernameUnique(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        String url = SUPABASE_URL + "?username=eq." + username + "&select=username";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(AdminAddActivity.this,
                                "Ошибка подключения при проверке логина", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();

                            if (!responseBody.equals("[]")) {
                                Toast.makeText(AdminAddActivity.this,
                                        "Логин уже занят", Toast.LENGTH_SHORT).show();
                            } else {

                                addNewAdmin(username, password);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(AdminAddActivity.this,
                                    "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AdminAddActivity.this,
                                "Ошибка проверки логина: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void addNewAdmin(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject adminData = new JSONObject();
            adminData.put("username", username);
            adminData.put("password", password);
            adminData.put("role", "Admin");

            RequestBody body = RequestBody.create(
                    adminData.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL)
                    .post(body)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(AdminAddActivity.this,
                                    "Ошибка подключения", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminAddActivity.this,
                                    "Администратор добавлен", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AdminAddActivity.this,
                                    "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}