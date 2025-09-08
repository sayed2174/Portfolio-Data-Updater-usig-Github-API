package com.example.githubcrudapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;

public class FileDetailActivity extends AppCompatActivity {
    public static final int NESTED_REQUEST = 2002;

    String owner, repo, pat, path, sha;
    boolean localPreview;
    RecyclerView rvKV;
    ProgressBar progress;
    ArrayList<HashMap<String,String>> kvList = new ArrayList<>();
    JsonKVAdapter kvAdapter;
    Object originalContentObj = null;
    boolean nestedMode = false;
    String nestedJsonString = null;

    // ADDED: This variable will track which list item is being edited in a nested activity.
    private int activeNestedEditPosition = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        owner = getIntent().getStringExtra("owner");
        repo = getIntent().getStringExtra("repo");
        pat = getIntent().getStringExtra("pat");
        path = getIntent().getStringExtra("path");
        sha = getIntent().getStringExtra("sha");
        localPreview = getIntent().getBooleanExtra("local_preview", false);
        nestedMode = getIntent().getBooleanExtra("nested_mode", false);
        nestedJsonString = getIntent().getStringExtra("json_content");

        rvKV = findViewById(R.id.rvKV);
        progress = findViewById(R.id.progressDetail);
        rvKV.setLayoutManager(new LinearLayoutManager(this));
        rvKV.setItemAnimator(new DefaultItemAnimator());

        // UPDATED: Pass the activity instance ('this') to the adapter's constructor.
        kvAdapter = new JsonKVAdapter(kvList, this);
        rvKV.setAdapter(kvAdapter);

        FloatingActionButton fabAdd = findViewById(R.id.btnAddField);
        FloatingActionButton fabSave = findViewById(R.id.btnSave);
        fabAdd.setOnClickListener(v -> showAddDialog());
        fabSave.setOnClickListener(v -> showCommitDialog());

        if (nestedMode) {
            try {
                Object parsed = tryParseJson(nestedJsonString);
                originalContentObj = parsed;
                buildKVListFromObject(parsed);
                kvAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Snackbar.make(rvKV, "Invalid nested JSON", Snackbar.LENGTH_LONG).show();
            }
        } else {
            fetchContent();
        }
    }

    // ADDED: A public method for the adapter to call before starting a nested edit.
    public void setActiveNestedEditPosition(int position) {
        this.activeNestedEditPosition = position;
    }

    // UPDATED: This method now correctly identifies and updates the right list item.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NESTED_REQUEST && resultCode == RESULT_OK && data != null) {
            String updatedJson = data.getStringExtra("nested_result");

            if (activeNestedEditPosition >= 0 && activeNestedEditPosition < kvList.size()) {
                HashMap<String,String> itemToUpdate = kvList.get(activeNestedEditPosition);
                itemToUpdate.put("value", updatedJson);
                itemToUpdate.put("type", detectType(tryParseJson(updatedJson)));
                kvAdapter.notifyItemChanged(activeNestedEditPosition);

                // Reset the tracker after the update is done.
                activeNestedEditPosition = -1;
            }
        }
    }

    // --- The rest of the methods in this file remain unchanged ---

    private void fetchContent() {
        progress.setVisibility(View.VISIBLE);
        if (localPreview) {
            try {
                java.io.InputStream is = getAssets().open(path);
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String raw = s.hasNext() ? s.next() : "";
                Object parsed = tryParseJson(raw);
                originalContentObj = parsed;
                buildKVListFromObject(parsed);
                kvAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Snackbar.make(rvKV, "Local load failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            } finally {
                progress.setVisibility(View.GONE);
            }
            return;
        }

        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("github_api");
                PyObject res = module.callAttr("get_file_content", owner, repo, path, pat, (Object) null);
                String jsonStr = res.toString();
                JSONObject obj = new JSONObject(jsonStr);
                String raw = obj.optString("raw", "");
                sha = obj.optString("sha", sha);
                Object parsed = tryParseJson(raw);
                originalContentObj = parsed;
                runOnUiThread(() -> {
                    buildKVListFromObject(parsed);
                    kvAdapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                Log.e("FileDetail", "fetch error", e);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Snackbar.make(rvKV, "Fetch failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private Object tryParseJson(String raw) {
        try { return new JSONObject(raw); }
        catch (JSONException e) {
            try { return new JSONArray(raw); }
            catch (JSONException e2) { return raw; }
        }
    }

    private void buildKVListFromObject(Object obj) {
        kvList.clear();
        if (obj instanceof JSONObject) {
            JSONObject jo = (JSONObject) obj;
            JSONArray names = jo.names();
            if (names != null) {
                for (int i=0;i<names.length();i++) {
                    String key = names.optString(i);
                    Object val = jo.opt(key);
                    HashMap<String,String> m = new HashMap<>();
                    m.put("key", key);
                    m.put("value", val == null ? "null" : val.toString());
                    m.put("type", detectType(val));
                    kvList.add(m);
                }
            }
        } else if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            for (int i=0;i<arr.length();i++) {
                Object val = arr.opt(i);
                HashMap<String,String> m = new HashMap<>();
                m.put("key", "["+i+"]");
                m.put("value", val == null ? "null" : val.toString());
                m.put("type", detectType(val));
                kvList.add(m);
            }
        } else {
            HashMap<String,String> m = new HashMap<>();
            m.put("key", "value");
            m.put("value", obj.toString());
            m.put("type", "PRIMITIVE");
            kvList.add(m);
        }
    }

    private String detectType(Object val) {
        if (val == null) return "PRIMITIVE";
        if (val instanceof JSONObject) return "OBJECT";
        if (val instanceof JSONArray) return "ARRAY";
        return "PRIMITIVE";
    }

    private void showAddDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_field, null);
        EditText etKey = v.findViewById(R.id.etKey);
        EditText etVal = v.findViewById(R.id.etVal);
        new AlertDialog.Builder(this)
                .setTitle("Add field")
                .setView(v)
                .setPositiveButton("Add", (dialog, which) -> {
                    String k = etKey.getText().toString().trim();
                    String val = etVal.getText().toString().trim();
                    if (k.isEmpty()) { Snackbar.make(rvKV, "Key required", Snackbar.LENGTH_SHORT).show(); return; }
                    HashMap<String,String> m = new HashMap<>();
                    m.put("key", k);
                    m.put("value", val);
                    m.put("type", "PRIMITIVE");
                    kvList.add(m);
                    kvAdapter.notifyItemInserted(kvList.size()-1);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCommitDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_commit, null);
        EditText etMsg = v.findViewById(R.id.etCommitMessage);
        EditText etBranch = v.findViewById(R.id.etBranchName);
        new AlertDialog.Builder(this)
                .setTitle(nestedMode ? "Return nested edits" : "Commit changes")
                .setView(v)
                .setPositiveButton(nestedMode ? "Return" : "Preview", (dialog, which) -> {
                    String msg = etMsg.getText().toString().trim();
                    String branch = etBranch.getText().toString().trim();
                    if (nestedMode) {
                        Object newObj = buildObjectFromKVList();
                        Intent out = new Intent();
                        out.putExtra("nested_result", newObj.toString());
                        setResult(Activity.RESULT_OK, out);
                        finish();
                        return;
                    }
                    Object newObj = buildObjectFromKVList();
                    String pretty = toPrettyJsonString(newObj);
                    showPreviewAndCommit(pretty, newObj, msg, branch);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Object buildObjectFromKVList() {
        if (originalContentObj instanceof JSONObject || !(originalContentObj instanceof JSONArray)) {
            JSONObject jo = new JSONObject();
            try {
                for (HashMap<String,String> m : kvList) {
                    String k = m.get("key");
                    String v = m.get("value");
                    String type = m.getOrDefault("type","PRIMITIVE");
                    Object val = tryParseJsonValue(v, type);
                    jo.put(k, val);
                }
            } catch (JSONException e) { /* ignore for now */ }
            return jo;
        } else {
            JSONArray arr = new JSONArray();
            try {
                for (HashMap<String,String> m : kvList) {
                    String v = m.get("value");
                    Object val = tryParseJsonValue(v, m.getOrDefault("type","PRIMITIVE"));
                    arr.put(val);
                }
            } catch (Exception e) {}
            return arr;
        }
    }

    private Object tryParseJsonValue(String v, String type) {
        if (type.equals("OBJECT") || type.equals("ARRAY")) {
            try { return tryParseJson(v); }
            catch (Exception ignored) {}
        }
        if ("null".equalsIgnoreCase(v)) return JSONObject.NULL;
        if ("true".equalsIgnoreCase(v)) return true;
        if ("false".equalsIgnoreCase(v)) return false;
        try {
            if (v.contains(".")) return Double.parseDouble(v);
            return Integer.parseInt(v);
        } catch (Exception ignored) {}
        return v;
    }

    private String toPrettyJsonString(Object obj) {
        try {
            if (obj instanceof JSONObject) return ((JSONObject)obj).toString(2);
            if (obj instanceof JSONArray) return ((JSONArray)obj).toString(2);
            return obj.toString();
        } catch (Exception e) { return obj.toString(); }
    }

    private void showPreviewAndCommit(String prettyJson, Object newObj, String commitMsg, String branch) {
        View v = getLayoutInflater().inflate(R.layout.dialog_preview, null);
        EditText etPreview = v.findViewById(R.id.etPreview);
        etPreview.setText(prettyJson);
        new AlertDialog.Builder(this)
                .setTitle("Commit preview")
                .setView(v)
                .setPositiveButton("Commit", (d, w) -> {
                    doCommit(newObj, commitMsg.isEmpty() ? "Updated via app" : commitMsg, branch.isEmpty() ? null : branch);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doCommit(Object newObj, String message, String branch) {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("github_api");
                PyObject res = module.callAttr("update_file", owner, repo, path, pat, newObj.toString(), sha, message, branch);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Snackbar.make(rvKV, "Committed", Snackbar.LENGTH_LONG).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Snackbar.make(rvKV, "Commit failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}