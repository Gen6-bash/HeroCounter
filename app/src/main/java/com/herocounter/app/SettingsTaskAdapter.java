package com.herocounter.app;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class SettingsTaskAdapter extends RecyclerView.Adapter<SettingsTaskAdapter.VH> {

    public interface Listener {
        void onEdit(Task task);
        void onDelete(Task task);
        void onOrderChanged(List<Task> reorderedList);
    }

    private List<Task> tasks;
    private final Listener listener;
    private ItemTouchHelper touchHelper;

    public SettingsTaskAdapter(List<Task> tasks, Listener listener) {
        this.tasks    = tasks;
        this.listener = listener;
    }

    public void attachTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settings_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Task task = tasks.get(position);
        holder.tvName.setText(task.name);
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(task));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(task));

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (touchHelper != null) touchHelper.startDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    public void onItemMoved(int from, int to) {
        Collections.swap(tasks, from, to);
        notifyItemMoved(from, to);
    }

    public void onDragComplete() {
        listener.onOrderChanged(tasks);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, btnEdit, btnDelete, dragHandle;
        VH(@NonNull View v) {
            super(v);
            tvName     = v.findViewById(R.id.tvSettingsTaskName);
            btnEdit    = v.findViewById(R.id.btnEditTask);
            btnDelete  = v.findViewById(R.id.btnDeleteTask);
            dragHandle = v.findViewById(R.id.dragHandle);
        }
    }
}
