package com.termux.app.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.DiffLine;

import java.util.ArrayList;
import java.util.List;

public class DiffLineAdapter extends RecyclerView.Adapter<DiffLineAdapter.DiffViewHolder> {

    private final List<DiffLine> lines = new ArrayList<>();

    public void submitList(List<DiffLine> newLines) {
        lines.clear();
        if (newLines != null) {
            lines.addAll(newLines);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diff_line, parent, false);
        return new DiffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiffViewHolder holder, int position) {
        DiffLine line = lines.get(position);

        // Line numbers
        holder.oldLineNum.setText(line.getOldLineNumber() > 0 ? String.valueOf(line.getOldLineNumber()) : "");
        holder.newLineNum.setText(line.getNewLineNumber() > 0 ? String.valueOf(line.getNewLineNumber()) : "");

        // Content
        holder.diffContent.setText(line.getContent());

        // Style based on type
        int bgColor, textColor, prefixColor;
        String prefix;

        switch (line.getType()) {
            case ADD:
                bgColor = R.color.diff_add_background;
                textColor = R.color.diff_add_text;
                prefixColor = R.color.diff_add_line_number;
                prefix = "+";
                holder.oldLineNum.setText("");
                holder.newLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_add_line_number));
                break;
            case REMOVE:
                bgColor = R.color.diff_remove_background;
                textColor = R.color.diff_remove_text;
                prefixColor = R.color.diff_remove_line_number;
                prefix = "-";
                holder.newLineNum.setText("");
                holder.oldLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_remove_line_number));
                break;
            case HUNK_HEADER:
                bgColor = R.color.diff_hunk_background;
                textColor = R.color.diff_hunk_text;
                prefixColor = R.color.diff_hunk_text;
                prefix = "@@";
                holder.diffContent.setTypeface(null, Typeface.BOLD);
                holder.oldLineNum.setText("");
                holder.newLineNum.setText("");
                break;
            case FILE_HEADER:
                bgColor = R.color.diff_file_header_background;
                textColor = R.color.diff_file_header_text;
                prefixColor = R.color.diff_file_header_text;
                prefix = "";
                holder.diffContent.setTypeface(null, Typeface.BOLD);
                holder.oldLineNum.setText("");
                holder.newLineNum.setText("");
                break;
            case NO_NEWLINE:
                bgColor = android.R.color.transparent;
                textColor = R.color.diff_no_newline_text;
                prefixColor = R.color.diff_no_newline_text;
                prefix = "";
                holder.diffContent.setTypeface(null, Typeface.ITALIC);
                holder.oldLineNum.setText("");
                holder.newLineNum.setText("");
                break;
            default: // CONTEXT
                bgColor = R.color.diff_context_background;
                textColor = R.color.diff_context_text;
                prefixColor = R.color.diff_line_number;
                prefix = " ";
                holder.oldLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_line_number));
                holder.newLineNum.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.diff_line_number));
                break;
        }

        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), bgColor));
        holder.diffContent.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColor));
        holder.diffPrefix.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), prefixColor));
        holder.diffPrefix.setText(prefix);

        // Reset typeface for non-header lines
        if (line.getType() != DiffLine.Type.HUNK_HEADER
                && line.getType() != DiffLine.Type.FILE_HEADER
                && line.getType() != DiffLine.Type.NO_NEWLINE) {
            holder.diffContent.setTypeface(Typeface.MONOSPACE);
        }
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class DiffViewHolder extends RecyclerView.ViewHolder {
        TextView oldLineNum;
        TextView newLineNum;
        TextView diffPrefix;
        TextView diffContent;

        DiffViewHolder(@NonNull View itemView) {
            super(itemView);
            oldLineNum = itemView.findViewById(R.id.old_line_number);
            newLineNum = itemView.findViewById(R.id.new_line_number);
            diffPrefix = itemView.findViewById(R.id.diff_prefix);
            diffContent = itemView.findViewById(R.id.diff_content);
            // Ensure text selection works in RecyclerView (parent intercepts touch events by default)
            diffContent.setTextIsSelectable(true);
        }
    }
}
