package com.example.crm1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class HomeActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://ecdgohdfcfumamdpxtmr.supabase.co/rest/v1/requests";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZGdvaGRmY2Z1bWFtZHB4dG1yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwNDMwMzAsImV4cCI6MjA2MzYxOTAzMH0.PbRiIzxtZ600b89Q2tu0VE5gnr35HWTwPZogtPPmRNg";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String USER_ID_KEY = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUserId = prefs.getString(USER_ID_KEY, null);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }


        EditText requestNameEditText = findViewById(R.id.editTextText);
        EditText requestDescEditText = findViewById(R.id.editTextText2);
        Button createButton = findViewById(R.id.button2);
        Button backButton = findViewById(R.id.button);
        Button allreq = findViewById(R.id.allReq);

        createButton.setOnClickListener(v -> {
            try {
                String name = requestNameEditText.getText().toString().trim();
                String description = requestDescEditText.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название заявки", Toast.LENGTH_SHORT).show();
                    return;
                }

                createNewRequest(currentUserId, name, description);
            } catch (Exception e) {
                Log.e("APP_CRASH", "Ошибка при создании заявки", e);
                Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
        });

        allreq.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RequestActivity.class));
            finish();
        });

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, FAQActivity.class);
                startActivity(intent);
            }
        });
    }

    private void createNewRequest(String userId, String name, String description) {
        OkHttpClient client = new OkHttpClient();

        try {

            JSONObject requestData = new JSONObject();
            requestData.put("name", name);


            if (!description.isEmpty()) {
                requestData.put("description", description);
            }


            if (!userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                Toast.makeText(this, "Ошибка ID пользователя", Toast.LENGTH_SHORT).show();
                return;
            }
            requestData.put("iduser", userId);
            requestData.put("status", "На рассмотрении");

            Log.d("REQUEST_DEBUG", "Отправляемые данные: " + requestData.toString());


            RequestBody body = RequestBody.create(
                    requestData.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL)
                    .post(body)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeActivity.this,
                                "Сетевая ошибка: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String rawResponse = response.body().string();
                    Log.d("RESPONSE_DEBUG", "Код: " + response.code() + ", Ответ: " + rawResponse);

                    runOnUiThread(() -> {
                        try {
                            if (response.isSuccessful()) {
                                Toast.makeText(HomeActivity.this,
                                        "Заявка успешно создана!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                JSONObject error = new JSONObject(rawResponse);
                                String message = error.optString("message", "Неизвестная ошибка");
                                Toast.makeText(HomeActivity.this,
                                        "Ошибка: " + message,
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(HomeActivity.this,
                                    "Ошибка разбора ответа: " + rawResponse,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    response.close();
                }
            });
        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Ошибка формирования JSON", e);
            Toast.makeText(this, "Ошибка создания данных", Toast.LENGTH_SHORT).show();
        }
    }
}