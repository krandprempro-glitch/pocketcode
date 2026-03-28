package com.termux.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import com.termux.app.models.ScriptItem;
import java.util.List;

public class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.ScriptViewHolder> {

    public interface OnScriptClickListener {
        void onScriptClick(ScriptItem script);
    }

    private final List<ScriptItem> scripts;
    private final OnScriptClickListener listener;

    public ScriptAdapter(List<ScriptItem> scripts, OnScriptClickListener listener) {
        this.scripts = scripts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScriptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_script, parent, false);
        return new ScriptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScriptViewHolder holder, int position) {
        ScriptItem script = scripts.get(position);
        holder.bind(script);
    }

    @Override
    public int getItemCount() {
        return scripts.size();
    }

    class ScriptViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView descText;

        ScriptViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.script_name);
            descText = itemView.findViewById(R.id.script_description);
        }

        void bind(ScriptItem script) {
            nameText.setText(script.getName());
            descText.setText(script.getDescription());
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScriptClick(script);
                }
            });
        }
    }
}
