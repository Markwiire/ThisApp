package com.example.crm1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://ecdgohdfcfumamdpxtmr.supabase.co/rest/v1/users";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZGdvaGRmY2Z1bWFtZHB4dG1yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwNDMwMzAsImV4cCI6MjA2MzYxOTAzMH0.PbRiIzxtZ600b89Q2tu0VE5gnr35HWTwPZogtPPmRNg";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String USER_ID_KEY = "user_id";
    private static final String USER_ROLE_KEY = "user_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etLogin = findViewById(R.id.username);
        EditText etPassword = findViewById(R.id.password);
        Button btnLogin = findViewById(R.id.buttonLogin);
        TextView tvToRegister = findViewById(R.id.reg);

        btnLogin.setOnClickListener(v -> {
            String username = etLogin.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            checkUserCredentials(username, password);
        });

        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void checkUserCredentials(String username, String password) {
        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL + "?username=eq." + username + "&select=id,password,role";

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
                    Toast.makeText(MainActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
                    Log.e("AUTH", "Connection error", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Ошибка сервера", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONArray users = new JSONArray(responseData);

                    if (users.length() == 0) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    JSONObject user = users.getJSONObject(0);
                    String dbPassword = user.getString("password");
                    String userId = user.getString("id");
                    String role = user.getString("role");

                    if (dbPassword.equals(password)) {
                        saveUserData(userId, role);
                        redirectUser(role);
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Неверный пароль", Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    Log.e("AUTH", "JSON parsing error", e);
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Ошибка данных", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void saveUserData(String userId, String role) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USER_ID_KEY, userId);
        editor.putString(USER_ROLE_KEY, role);
        editor.apply();
    }

    private void redirectUser(String role) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Авторизация успешна", Toast.LENGTH_SHORT).show();

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String userId = prefs.getString(USER_ID_KEY, "");
            Intent intent;
            if ("Admin".equalsIgnoreCase(role)) {
                intent = new Intent(this, administrator.class);
            } else {
                intent = new Intent(this, HomeActivity.class);
            }
            intent.putExtra("USER_ID", prefs.getString(USER_ID_KEY, ""));

            startActivity(intent);
            finish();
        });
    }
}
