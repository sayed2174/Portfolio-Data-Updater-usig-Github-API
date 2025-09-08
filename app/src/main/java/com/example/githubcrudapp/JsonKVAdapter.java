package com.example.githubcrudapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class JsonKVAdapter extends RecyclerView.Adapter<JsonKVAdapter.VH> {
    ArrayList<HashMap<String, String>> items;
    private FileDetailActivity activity;

    public JsonKVAdapter(ArrayList<HashMap<String, String>> items, FileDetailActivity activity) {
        this.items = items;
        this.activity = activity;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_value, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        HashMap<String, String> map = items.get(position);
        holder.tvValue.setText(map.get("value"));
        String type = map.getOrDefault("type", "PRIMITIVE");

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            if ("PRIMITIVE".equals(type)) {
                showEditDialog(v.getContext(), currentPosition);
            } else {
                activity.setActiveNestedEditPosition(currentPosition);
                Intent i = new Intent(v.getContext(), FileDetailActivity.class);
                i.putExtra("nested_mode", true);
                i.putExtra("json_content", map.get("value"));
                ((Activity) v.getContext()).startActivityForResult(i, FileDetailActivity.NESTED_REQUEST);
            }
        });

        holder.btnMenu.setOnClickListener(v -> showPopupMenu(v, holder.getAdapterPosition()));

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            items.remove(currentPosition);
            notifyItemRemoved(currentPosition);
            notifyItemRangeChanged(currentPosition, items.size());
        });
    }

    private void showPopupMenu(View view, int position) {
        if (position == RecyclerView.NO_POSITION) return;
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenuInflater().inflate(R.menu.row_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_copy) {
                copyToClipboard(view.getContext(), items.get(position));
                return true;
            } else if (itemId == R.id.menu_duplicate_above) {
                duplicateItem(position, true);
                return true;
            } else if (itemId == R.id.menu_duplicate_below) {
                duplicateItem(position, false);
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }

    private void copyToClipboard(Context context, HashMap<String, String> item) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        String key = item.get("key");
        String value = item.get("value");
        String escapedValue = value.replace("\"", "\\\"");
        String clipText = "{\"" + key + "\": \"" + escapedValue + "\"}";
        ClipData clip = ClipData.newPlainText("JSON Key-Value", clipText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void duplicateItem(int position, boolean above) {
        if (position < 0 || position >= items.size()) return;
        HashMap<String, String> originalItem = items.get(position);
        HashMap<String, String> newItem = new HashMap<>(originalItem);

        int insertPosition = above ? position : position + 1;
        items.add(insertPosition, newItem);
        notifyItemInserted(insertPosition);
        notifyItemRangeChanged(insertPosition, items.size());
    }

    private void showEditDialog(Context context, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_kv, null);
        EditText etKey = dialogView.findViewById(R.id.etEditKey);
        EditText etValue = dialogView.findViewById(R.id.etEditValue);

        HashMap<String, String> currentItem = items.get(position);
        etKey.setText(currentItem.get("key"));
        etValue.setText(currentItem.get("value"));

        new AlertDialog.Builder(context)
                .setTitle("Edit Field")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newKey = etKey.getText().toString().trim();
                    String newValue = etValue.getText().toString().trim();
                    currentItem.put("key", newKey);
                    currentItem.put("value", newValue);
                    notifyItemChanged(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvValue;
        ImageButton btnMenu;
        ImageButton btnDelete;

        VH(View itemView) {
            super(itemView);
            tvValue = itemView.findViewById(R.id.tvValue);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}