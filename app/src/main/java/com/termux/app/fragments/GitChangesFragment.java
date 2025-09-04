package com.termux.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.termux.R;

public class GitChangesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 占位布局，显示Git变更功能
        View view = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
        android.widget.TextView textView = view.findViewById(android.R.id.text1);
        textView.setText("Git变更功能\n(待实现)");
        textView.setGravity(android.view.Gravity.CENTER);
        return view;
    }
}