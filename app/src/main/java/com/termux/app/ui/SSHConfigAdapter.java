package com.termux.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;
import com.termux.app.models.SSHConnectionConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * SSH配置列表适配器
 */
public class SSHConfigAdapter extends RecyclerView.Adapter<SSHConfigAdapter.ConfigViewHolder> {

    private final Context mContext;
    private final List<SSHConnectionConfig> mConfigs;
    private OnConfigClickListener mListener;

    public interface OnConfigClickListener {
        void onConfigClick(SSHConnectionConfig config);
        void onConfigDelete(SSHConnectionConfig config);
        void onConfigConnect(SSHConnectionConfig config);
        void onConfigEdit(SSHConnectionConfig config);
        void onConfigTest(SSHConnectionConfig config);
    }

    public SSHConfigAdapter(Context context) {
        mContext = context;
        mConfigs = new ArrayList<>();
    }

    @NonNull
    @Override
    public ConfigViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_ssh_config, parent, false);
        return new ConfigViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConfigViewHolder holder, int position) {
        SSHConnectionConfig config = mConfigs.get(position);
        holder.bind(config);
    }

    @Override
    public int getItemCount() {
        return mConfigs.size();
    }

    public void updateConfigs(List<SSHConnectionConfig> configs) {
        mConfigs.clear();
        if (configs != null) {
            mConfigs.addAll(configs);
        }
        notifyDataSetChanged();
    }

    public void setOnConfigClickListener(OnConfigClickListener listener) {
        mListener = listener;
    }

    class ConfigViewHolder extends RecyclerView.ViewHolder {
        private final TextView mNameText;
        private final TextView mDetailsText;
        private final ImageButton mConnectButton;
        private final ImageButton mDeleteButton;
        private final ImageButton mEditButton;
        private final ImageButton mTestButton;

        public ConfigViewHolder(@NonNull View itemView) {
            super(itemView);
            mNameText = itemView.findViewById(R.id.tv_config_name);
            mDetailsText = itemView.findViewById(R.id.tv_host_info);
            mConnectButton = itemView.findViewById(R.id.btn_use);
            mDeleteButton = itemView.findViewById(R.id.btn_delete);
            mEditButton = itemView.findViewById(R.id.btn_edit);
            mTestButton = itemView.findViewById(R.id.btn_test);
        }

        public void bind(SSHConnectionConfig config) {
            mNameText.setText(config.getName());
            mDetailsText.setText(config.getUsername() + "@" + config.getHost() + ":" + config.getPort());

            // 点击整个item加载配置到输入框
            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onConfigClick(config);
                }
            });

            // 连接按钮
            mConnectButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onConfigConnect(config);
                }
            });

            // 删除按钮
            mDeleteButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onConfigDelete(config);
                }
            });

            // 编辑按钮
            mEditButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onConfigEdit(config);
                }
            });

            // 测试按钮
            mTestButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onConfigTest(config);
                }
            });
        }
    }
}