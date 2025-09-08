package com.example.githubcrudapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;

public class RepoFileAdapter extends RecyclerView.Adapter<RepoFileAdapter.VH> {
    ArrayList<HashMap<String,String>> files;
    OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(HashMap<String,String> file);
    }

    public RepoFileAdapter(ArrayList<HashMap<String,String>> files, OnFileClickListener listener){
        this.files = files;
        this.listener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repo_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position){
        HashMap<String,String> file = files.get(position);
        holder.tvName.setText(file.get("name"));
        holder.tvPath.setText(file.get("path"));
        holder.card.setOnClickListener(v -> {
            if (listener!=null) listener.onFileClick(file);
        });

        holder.card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete file")
                    .setMessage("Delete " + file.get("name") + " ?")
                    .setPositiveButton("Delete", (d, w) -> {
                        // we need the activity to handle deletion (not included here)
                        Context ctx = v.getContext();
                        // Show a snackbar suggesting long-press delete action handled in activity
                        Snackbar.make(v, "Long-press delete is handled via RepoListActivity", Snackbar.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount(){ return files.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName, tvPath;
        VH(View itemView){
            super(itemView);
            card = itemView.findViewById(R.id.card);
            tvName = itemView.findViewById(R.id.tvName);
            tvPath = itemView.findViewById(R.id.tvPath);
        }
    }
}
