package com.example.crm1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

public class RequestActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://ecdgohdfcfumamdpxtmr.supabase.co/rest/v1/";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZGdvaGRmY2Z1bWFtZHB4dG1yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwNDMwMzAsImV4cCI6MjA2MzYxOTAzMH0.PbRiIzxtZ600b89Q2tu0VE5gnr35HWTwPZogtPPmRNg";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String USER_ID_KEY = "user_id";

    private TableLayout tableLayout;
    private Button btnBack, btnRes;
    private OkHttpClient client = new OkHttpClient();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home1);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(USER_ID_KEY, null);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        tableLayout = findViewById(R.id.tableLayout);
        btnBack = findViewById(R.id.btnBack);
        btnRes = findViewById(R.id.btn5);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnRes.setOnClickListener(v -> {
            Toast.makeText(this, "Обновление данных...", Toast.LENGTH_SHORT).show();
            loadUserRequests();
        });

        loadUserRequests();
    }

    private void loadUserRequests() {
        String url = SUPABASE_URL + "requests?iduser=eq." + currentUserId +
                "&select=id,iduser,name,description,datecreated,status";

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
                        Toast.makeText(RequestActivity.this,
                                "Ошибка загрузки заявок", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        JSONArray requests = new JSONArray(response.body().string());
                        runOnUiThread(() -> showRequests(requests));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showRequests(JSONArray requests) {
        tableLayout.removeViews(1, tableLayout.getChildCount() - 1);

        try {
            for (int i = 0; i < requests.length(); i++) {
                JSONObject req = requests.getJSONObject(i);
                addRequestRow(
                        req.getString("id"),
                        req.getString("iduser"),
                        req.getString("name"),
                        req.optString("description", ""),
                        req.optString("datecreated", ""),
                        req.optString("status", "Новая")
                );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addRequestRow(String id, String userId, String name,
                               String description, String date, String status) {
        TableRow row = new TableRow(this);


        row.addView(createTableCell(id));
        row.addView(createTableCell(userId));
        row.addView(createTableCell(name));
        row.addView(createTableCell(description));
        row.addView(createTableCell(date));
        row.addView(createTableCell(status));


        TextView commentCell = createTableCell("Загрузка...");
        row.addView(commentCell);
        loadCommentForRequest(id, commentCell);


        Button btnDelete = new Button(this);
        btnDelete.setText("Удалить");
        btnDelete.setBackgroundColor(0xFFFFFFFF); // Белый фон
        btnDelete.setTextColor(0xFF888888); // Серый текст
        btnDelete.setOnClickListener(v -> deleteRequest(id));
        row.addView(btnDelete);

        tableLayout.addView(row);
    }

    private void loadCommentForRequest(String requestId, TextView commentView) {
        String url = SUPABASE_URL + "comments?request_id=eq." + requestId +
                "&select=comment_text&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> commentView.setText("Ошибка загрузки"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray comments = new JSONArray(response.body().string());
                    String commentText = comments.length() > 0 ?
                            comments.getJSONObject(0).getString("comment_text") :
                            "No comments yet";
                    runOnUiThread(() -> commentView.setText(commentText));
                } catch (Exception e) {
                    runOnUiThread(() -> commentView.setText("Ошибка"));
                }
            }
        });
    }

    private void deleteRequest(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Удалить эту заявку?")
                .setPositiveButton("Да", (dialog, which) -> {
                    Request request = new Request.Builder()
                            .url(SUPABASE_URL + "requests?id=eq." + id)
                            .delete()
                            .addHeader("apikey", SUPABASE_API_KEY)
                            .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(RequestActivity.this,
                                            "Ошибка удаления", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            runOnUiThread(() -> {
                                if (response.isSuccessful()) {
                                    Toast.makeText(RequestActivity.this,
                                            "Заявка удалена", Toast.LENGTH_SHORT).show();
                                    loadUserRequests();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private TextView createTableCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        tv.setTextSize(14);
        return tv;
    }
}

