package com.example.githubcrudapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class RepoListActivity extends AppCompatActivity implements RepoFileAdapter.OnFileClickListener {
    RecyclerView rvFiles;
    ProgressBar progress;
    RepoFileAdapter adapter;
    ArrayList<HashMap<String,String>> files = new ArrayList<>();
    ArrayList<HashMap<String,String>> filesBackup = new ArrayList<>();
    String owner, repo, pat;
    boolean localPreview;
    FloatingActionButton fabCreate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_list);
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        rvFiles = findViewById(R.id.rvFiles);
        progress = findViewById(R.id.progress);
        fabCreate = findViewById(R.id.fabCreate);

        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setItemAnimator(new DefaultItemAnimator());
        adapter = new RepoFileAdapter(files, this);
        rvFiles.setAdapter(adapter);

        owner = getIntent().getStringExtra("owner");
        repo = getIntent().getStringExtra("repo");
        pat = getIntent().getStringExtra("pat");
        localPreview = getIntent().getBooleanExtra("local_preview", false);

        SearchView sv = findViewById(R.id.searchView);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });

        fabCreate.setOnClickListener(v -> showCreateDialog());

        fetchFiles();
    }

    private void fetchFiles() {
        progress.setVisibility(View.VISIBLE);
        if (localPreview) {
            // load assets/*.json to simulate files
            try {
                String[] assetFiles = getAssets().list("");
                files.clear();
                for (String name : new String[]{"intern.json","project.json","skill.json","cert.json","edu.json"}) {
                    try {
                        InputStream is = getAssets().open(name);
                        Scanner s = new Scanner(is).useDelimiter("\\A");
                        String content = s.hasNext() ? s.next() : "";
                        HashMap<String,String> map = new HashMap<>();
                        map.put("name", name);
                        map.put("path", name);
                        map.put("sha", "local");
                        map.put("download_url", "");
                        files.add(map);
                        is.close();
                    } catch (Exception ignored) {}
                }
                filesBackup.clear();
                filesBackup.addAll(files);
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                Snackbar.make(rvFiles, "Local preview load failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            } finally {
                progress.setVisibility(View.GONE);
            }
            return;
        }

        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("github_api");
                PyObject res = module.callAttr("list_json_files", owner, repo, pat, "");
                String jsonStr = res.toString();
                JSONArray arr = new JSONArray(jsonStr);
                files.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.getJSONObject(i);
                    HashMap<String,String> map = new HashMap<>();
                    map.put("name", o.getString("name"));
                    map.put("path", o.getString("path"));
                    map.put("sha", o.getString("sha"));
                    map.put("download_url", o.optString("download_url",""));
                    files.add(map);
                }
                filesBackup.clear();
                filesBackup.addAll(files);
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Snackbar.make(rvFiles, "Fetch failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void filter(String q) {
        q = q==null ? "" : q.toLowerCase();
        files.clear();
        for (HashMap<String,String> f : filesBackup) {
            if (f.get("name").toLowerCase().contains(q) || f.get("path").toLowerCase().contains(q)) {
                files.add(f);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onFileClick(HashMap<String,String> file) {
        Intent i = new Intent(this, FileDetailActivity.class);
        i.putExtra("owner", owner);
        i.putExtra("repo", repo);
        i.putExtra("pat", pat);
        i.putExtra("path", file.get("path"));
        i.putExtra("sha", file.get("sha"));
        i.putExtra("local_preview", localPreview);
        startActivityForResult(i, 1001);
    }

    private void showCreateDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_field, null);
        // reuse dialog for create (first field will be filename)
        new AlertDialog.Builder(this)
                .setTitle("Create JSON file")
                .setView(v)
                .setPositiveButton("Create", (dialog, which) -> {
                    EditText etKey = v.findViewById(R.id.etKey);
                    EditText etVal = v.findViewById(R.id.etVal);
                    String filename = etKey.getText().toString().trim();
                    String initial = etVal.getText().toString().trim();
                    if (!filename.endsWith(".json")) filename += ".json";
                    if (localPreview) {
                        Snackbar.make(rvFiles, "Local preview mode: file not created on GitHub", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    String finalFilename = filename;
                    String finalFilename1 = filename;
                    new Thread(() -> {
                        try {
                            // parse initial content as JSON if possible, else as empty object
                            Object contentObj;
                            try {
                                contentObj = new org.json.JSONObject(initial);
                            } catch (Exception e) {
                                try {
                                    contentObj = new org.json.JSONArray(initial);
                                } catch (Exception e2) {
                                    contentObj = new org.json.JSONObject();
                                }
                            }
                            Python py = Python.getInstance();
                            PyObject module = py.getModule("github_api");
                            PyObject res = module.callAttr("create_file", owner, repo, finalFilename1, pat, contentObj.toString(), "Create via app");
                            runOnUiThread(() -> {
                                Snackbar.make(rvFiles, "Created " + finalFilename, Snackbar.LENGTH_LONG).show();
                                fetchFiles();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Snackbar.make(rvFiles, "Create failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1001 && resultCode==RESULT_OK) {
            // refresh file list (in case save/create/delete happened)
            fetchFiles();
        }
    }
}
