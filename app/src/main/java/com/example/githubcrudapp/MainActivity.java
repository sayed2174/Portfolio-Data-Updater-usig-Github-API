package com.example.githubcrudapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.materialswitch.MaterialSwitch;


public class MainActivity extends AppCompatActivity {
    EditText etOwner, etRepo, etPat;
    Button btnOpen;
    MaterialSwitch swLocalPreview, swTheme;
    private static final String PREFS = "github_prefs_v2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme applied based on preference
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark", false);
        setTheme(dark ? R.style.Theme_GithubCrudApp_Dark : R.style.Theme_GithubCrudApp_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etOwner = findViewById(R.id.etOwner);
        etRepo = findViewById(R.id.etRepo);
        etPat = findViewById(R.id.etPat);
        btnOpen = findViewById(R.id.btnOpen);
        swLocalPreview = findViewById(R.id.swLocalPreview);
        swTheme = findViewById(R.id.swTheme);

        etOwner.setText(prefs.getString("owner", ""));
        etRepo.setText(prefs.getString("repo", ""));
        etPat.setText(prefs.getString("pat", ""));
        swLocalPreview.setChecked(prefs.getBoolean("local_preview", false));
        swTheme.setChecked(prefs.getBoolean("dark", dark));

        btnOpen.setOnClickListener(v -> {
            String owner = etOwner.getText().toString().trim();
            String repo = etRepo.getText().toString().trim();
            String pat = etPat.getText().toString().trim();
            boolean local = swLocalPreview.isChecked();

            if (owner.isEmpty() && !local) {
                etOwner.setError("Owner required (or enable Local Preview)");
                return;
            }
            if (repo.isEmpty() && !local) {
                etRepo.setError("Repo required (or enable Local Preview)");
                return;
            }
            prefs.edit()
                    .putString("owner", owner)
                    .putString("repo", repo)
                    .putString("pat", pat)
                    .putBoolean("local_preview", local)
                    .apply();

            Intent i = new Intent(MainActivity.this, RepoListActivity.class);
            i.putExtra("owner", owner);
            i.putExtra("repo", repo);
            i.putExtra("pat", pat);
            i.putExtra("local_preview", local);
            startActivity(i);
        });

        swTheme.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean("dark", isChecked).apply();
            // recreate to apply theme
            recreate();
        });
    }
}
