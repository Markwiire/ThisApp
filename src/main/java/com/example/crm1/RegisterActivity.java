package com.example.crm1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

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

public class RegisterActivity extends AppCompatActivity {

    private static final String SUPABASE_URL_users = "https://ecdgohdfcfumamdpxtmr.supabase.co/rest/v1/users";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZGdvaGRmY2Z1bWFtZHB4dG1yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwNDMwMzAsImV4cCI6MjA2MzYxOTAzMH0.PbRiIzxtZ600b89Q2tu0VE5gnr35HWTwPZogtPPmRNg";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private EditText NewLogin;
    private EditText NewPassword;
    private EditText RepeatPassword;
    private Button btnRegister;
    private TextView tvToLogin;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        NewLogin = findViewById(R.id.username);
        NewPassword = findViewById(R.id.password);
        RepeatPassword = findViewById(R.id.password2);
        btnRegister = findViewById(R.id.GoReg);
        tvToLogin = findViewById(R.id.log);


        btnRegister.setOnClickListener(v -> {
            String username = NewLogin.getText().toString().trim();
            String password = NewPassword.getText().toString();
            String repeatPassword = RepeatPassword.getText().toString();

            if (username.isEmpty()) {
                NewLogin.setError("Введите имя пользователя");
                return;
            }

            if (password.isEmpty()) {
                NewPassword.setError("Введите пароль");
                return;
            }

            if (repeatPassword.isEmpty()) {
                RepeatPassword.setError("Повторите пароль");

            if (password.length() < MIN_PASSWORD_LENGTH) {
                Toast.makeText(this, "Пароль должен содержать минимум " + MIN_PASSWORD_LENGTH + " символов", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$")) {
                Toast.makeText(this, "Пароль должен содержать буквы и цифры", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(repeatPassword)) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }
            checkUsernameUnique(username, password);
        });

        tvToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }
    private void checkUsernameUnique(String username, String password) {
        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL_users + "?username=eq." + username + "&select=username";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Ошибка соединения при проверке логина", Toast.LENGTH_SHORT).show();
                    Log.e("SupabaseRequest", "Ошибка при проверке логина", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            if (!responseBody.equals("[]")) {
                                Toast.makeText(RegisterActivity.this, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show();
                            } else {
                                registerUser(username, password);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(RegisterActivity.this, "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Ошибка проверки логина", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void registerUser(String username, String password) {
        JSONObject userData = new JSONObject();
        try {
            userData.put("username", username);
            userData.put("password", password);
            userData.put("role", "Client");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при создании данных", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                userData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(SUPABASE_URL_users)
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
                    Log.e("SupabaseRequest", "Ошибка при регистрации", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "empty body";
                    Log.e("SupabaseRequest", "Ошибка регистрации: " + response.code() + " - " + response.message() + " | " + errorBody);
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Ошибка при регистрации", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}

