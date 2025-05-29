package com.example.crm1;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AdminAllRequestsActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://ecdgohdfcfumamdpxtmr.supabase.co/rest/v1/";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZGdvaGRmY2Z1bWFtZHB4dG1yIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgwNDMwMzAsImV4cCI6MjA2MzYxOTAzMH0.PbRiIzxtZ600b89Q2tu0VE5gnr35HWTwPZogtPPmRNg";

    private TableLayout tableLayout;
    Button button6;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adminreq);

        tableLayout = findViewById(R.id.tableLayout);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnRes = findViewById(R.id.button6);

        btnBack.setOnClickListener(v -> finish());

        btnRes.setOnClickListener(v -> {
            Toast.makeText(this, "Обновление данных...", Toast.LENGTH_SHORT).show();
            loadAllRequests();
        });

        loadAllRequests();
    }

    private void loadAllRequests() {
        String url = SUPABASE_URL + "requests?select=id,iduser,name,status,description";

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
                        Toast.makeText(AdminAllRequestsActivity.this,
                                "Ошибка загрузки", Toast.LENGTH_SHORT).show());
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
        tableLayout.removeViews(1, Math.max(0, tableLayout.getChildCount() - 1));

        try {
            for (int i = 0; i < requests.length(); i++) {
                JSONObject request = requests.getJSONObject(i);
                String id = request.getString("id");
                String userId = request.getString("iduser");
                String name = request.getString("name");
                String status = request.optString("status", "На рассмотрении");

                addRequestToTable(id, userId, name, status);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addRequestToTable(String id, String userId, String name, String currentStatus) {
        TableRow row = new TableRow(this);
        final String requestId = id;


        row.addView(createTableCell(id));
        row.addView(createTableCell(userId));
        row.addView(createTableCell(name));


        Spinner statusSpinner = new Spinner(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);


        int position = adapter.getPosition(currentStatus);
        statusSpinner.setSelection(position);


        final String[] previousStatus = {currentStatus};

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newStatus = parent.getItemAtPosition(position).toString();
                if (!newStatus.equals(previousStatus[0])) {
                    updateRequestStatus(requestId, newStatus);
                    previousStatus[0] = newStatus;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        row.addView(statusSpinner);


        TextView commentCell = createTableCell("Загрузка...");
        row.addView(commentCell);
        loadComment(id, commentCell);


        Button btnComment = new Button(this);
        btnComment.setText("Добавить комментарий");
        btnComment.setBackgroundColor(0xFFFFFFFF);
        btnComment.setTextColor(0xFF888888);
        btnComment.setOnClickListener(v -> showAddCommentDialog(id));
        row.addView(btnComment);

        tableLayout.addView(row);
    }

    private void updateRequestStatus(String requestId, String newStatus) {
        try {
            JSONObject statusData = new JSONObject();
            statusData.put("status", newStatus);

            RequestBody body = RequestBody.create(
                    statusData.toString(),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "requests?id=eq." + requestId)
                    .patch(body)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(AdminAllRequestsActivity.this,
                                    "Ошибка обновления статуса", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminAllRequestsActivity.this,
                                    "Статус обновлен: " + newStatus, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadComment(String requestId, TextView commentView) {
        String url = SUPABASE_URL + "comments?request_id=eq." + requestId + "&select=comment_text&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> commentView.setText("Ошибка"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray comments = new JSONArray(response.body().string());
                    String commentText = comments.length() > 0 ?
                            comments.getJSONObject(0).getString("comment_text") :
                            "Нет комментария";
                    runOnUiThread(() -> commentView.setText(commentText));
                } catch (Exception e) {
                    runOnUiThread(() -> commentView.setText("Ошибка"));
                }
            }
        });
    }

    private void showAddCommentDialog(String requestId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить комментарий");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String comment = input.getText().toString();
            if (!comment.isEmpty()) {
                saveComment(requestId, comment);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void saveComment(String requestId, String commentText) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String adminId = prefs.getString("user_id", "");

        try {
            JSONObject comment = new JSONObject();
            comment.put("request_id", requestId);
            comment.put("admin_id", adminId);
            comment.put("comment_text", commentText);

            RequestBody body = RequestBody.create(
                    comment.toString(),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "comments")
                    .post(body)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(AdminAllRequestsActivity.this,
                                    "Ошибка сохранения", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminAllRequestsActivity.this,
                                    "Комментарий сохранен", Toast.LENGTH_SHORT).show();
                            loadAllRequests();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private TextView createTableCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        return tv;
    }
}