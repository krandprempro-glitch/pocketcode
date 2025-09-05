package com.termux.filebrowser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;
import com.termux.app.models.DrawerMenuItem;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽屉菜单适配器
 * 支持可折叠的菜单项和子菜单
 */
public class DrawerMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String LOG_TAG = "DrawerMenuAdapter";
    
    private static final int TYPE_MENU_ITEM = 1;
    private static final int TYPE_SUBMENU_ITEM = 2;
    
    private Context context;
    private List<DrawerMenuItem> menuItems;
    private OnMenuItemClickListener listener;
    
    public interface OnMenuItemClickListener {
        void onMenuItemClick(DrawerMenuItem item);
        void onSubMenuItemClick(DrawerMenuItem parentItem, DrawerMenuItem subItem);
    }
    
    public DrawerMenuAdapter(Context context, OnMenuItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.menuItems = new ArrayList<>();
    }
    
    public void updateMenuItems(List<DrawerMenuItem> items) {
        this.menuItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        DrawerMenuItem item = menuItems.get(position);
        return item.isSubMenu() ? TYPE_SUBMENU_ITEM : TYPE_MENU_ITEM;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SUBMENU_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_submenu, parent, false);
            return new SubMenuViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_menu, parent, false);
            return new MenuViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DrawerMenuItem item = menuItems.get(position);
        
        if (holder instanceof MenuViewHolder) {
            bindMenuViewHolder((MenuViewHolder) holder, item, position);
        } else if (holder instanceof SubMenuViewHolder) {
            bindSubMenuViewHolder((SubMenuViewHolder) holder, item, position);
        }
    }
    
    private void bindMenuViewHolder(MenuViewHolder holder, DrawerMenuItem item, int position) {
        holder.menuText.setText(item.getTitle());
        holder.menuIcon.setImageResource(item.getIconRes());
        
        // 设置展开指示器
        if (item.hasSubItems()) {
            holder.expandIndicator.setVisibility(View.VISIBLE);
            // 根据展开状态旋转指示器
            float rotation = item.isExpanded() ? 90f : 0f;
            holder.expandIndicator.setRotation(rotation);
        } else {
            holder.expandIndicator.setVisibility(View.GONE);
        }
        
        // 点击处理
        holder.itemView.setOnClickListener(v -> {
            Logger.logInfo(LOG_TAG, "Menu item clicked: " + item.getTitle() + ", hasSubItems: " + item.hasSubItems());
            if (item.hasSubItems()) {
                toggleExpansion(item, position);
            } else {
                if (listener != null) {
                    Logger.logInfo(LOG_TAG, "Calling onMenuItemClick for: " + item.getTitle());
                    listener.onMenuItemClick(item);
                } else {
                    Logger.logError(LOG_TAG, "Listener is null for menu click");
                }
            }
        });
    }
    
    private void bindSubMenuViewHolder(SubMenuViewHolder holder, DrawerMenuItem item, int position) {
        holder.submenuText.setText(item.getTitle());
        holder.submenuIcon.setImageResource(item.getIconRes());
        
        // 设置徽章文本
        if (item.getBadgeText() != null && !item.getBadgeText().isEmpty()) {
            holder.submenuBadge.setVisibility(View.VISIBLE);
            holder.submenuBadge.setText(item.getBadgeText());
        } else {
            holder.submenuBadge.setVisibility(View.GONE);
        }
        
        // 点击处理
        holder.itemView.setOnClickListener(v -> {
            Logger.logInfo(LOG_TAG, "SubMenu item clicked: " + item.getTitle() + ", action: " + item.getAction());
            if (listener != null) {
                DrawerMenuItem parentItem = findParentItem(item);
                Logger.logInfo(LOG_TAG, "Calling onSubMenuItemClick with parent: " + 
                    (parentItem != null ? parentItem.getTitle() : "null") + ", sub: " + item.getTitle());
                listener.onSubMenuItemClick(parentItem, item);
            } else {
                Logger.logError(LOG_TAG, "Listener is null for submenu click");
            }
        });
    }
    
    /**
     * 切换菜单项的展开/折叠状态
     */
    private void toggleExpansion(DrawerMenuItem item, int position) {
        boolean wasExpanded = item.isExpanded();
        item.setExpanded(!wasExpanded);
        
        if (wasExpanded) {
            // 折叠：移除子项
            collapseMenuItem(item, position);
        } else {
            // 展开：添加子项
            expandMenuItem(item, position);
        }
        
        // 旋转展开指示器
        animateExpandIndicator(position, !wasExpanded);
    }
    
    /**
     * 展开菜单项
     */
    private void expandMenuItem(DrawerMenuItem item, int position) {
        List<DrawerMenuItem> subItems = item.getSubItems();
        if (subItems != null && !subItems.isEmpty()) {
            // 标记所有子项为子菜单
            for (DrawerMenuItem subItem : subItems) {
                subItem.setSubMenu(true);
                subItem.setParentId(item.getId());
            }
            
            // 在当前位置后插入子项
            menuItems.addAll(position + 1, subItems);
            notifyItemRangeInserted(position + 1, subItems.size());
            
            Logger.logInfo(LOG_TAG, "Expanded menu: " + item.getTitle() + " with " + subItems.size() + " sub items");
        }
    }
    
    /**
     * 折叠菜单项，支持递归折叠子项的子项
     */
    private void collapseMenuItem(DrawerMenuItem item, int position) {
        List<Integer> toRemove = new ArrayList<>();
        
        // 递归查找所有需要移除的子项位置
        findItemsToRemove(item, position + 1, toRemove);
        
        // 从后往前移除，避免索引变化
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            int indexToRemove = toRemove.get(i);
            DrawerMenuItem removedItem = menuItems.get(indexToRemove);
            
            // 如果被移除的项是展开状态，先将其标记为折叠
            if (removedItem.isExpanded()) {
                removedItem.setExpanded(false);
            }
            
            menuItems.remove(indexToRemove);
        }
        
        if (!toRemove.isEmpty()) {
            int removeCount = toRemove.size();
            notifyItemRangeRemoved(position + 1, removeCount);
            Logger.logInfo(LOG_TAG, "Collapsed menu: " + item.getTitle() + ", removed " + removeCount + " sub items");
        }
    }
    
    /**
     * 递归查找所有需要移除的子项
     */
    private void findItemsToRemove(DrawerMenuItem parentItem, int startIndex, List<Integer> toRemove) {
        for (int i = startIndex; i < menuItems.size(); i++) {
            DrawerMenuItem currentItem = menuItems.get(i);
            
            // 如果是直接子项
            if (currentItem.isSubMenu() && parentItem.getId().equals(currentItem.getParentId())) {
                toRemove.add(i);
                
                // 如果该子项也是展开状态，递归查找它的子项
                if (currentItem.isExpanded()) {
                    findItemsToRemove(currentItem, i + 1, toRemove);
                }
            } else if (currentItem.isSubMenu()) {
                // 如果遇到不属于当前父项的子项，继续查找（可能是更深层的嵌套）
                continue;
            } else {
                // 遇到非子项，停止查找
                break;
            }
        }
    }
    
    /**
     * 查找子项的父菜单项（支持嵌套查找）
     */
    private DrawerMenuItem findParentItem(DrawerMenuItem subItem) {
        String parentId = subItem.getParentId();
        if (parentId != null) {
            // 先在所有菜单项中查找（包括子菜单项）
            for (DrawerMenuItem item : menuItems) {
                if (parentId.equals(item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * 动画展开指示器
     */
    private void animateExpandIndicator(int position, boolean expanded) {
        RecyclerView.ViewHolder holder = null; // 需要从RecyclerView获取
        if (holder instanceof MenuViewHolder) {
            ImageView indicator = ((MenuViewHolder) holder).expandIndicator;
            float fromRotation = expanded ? 0f : 90f;
            float toRotation = expanded ? 90f : 0f;
            
            RotateAnimation animation = new RotateAnimation(
                fromRotation, toRotation,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            animation.setDuration(200);
            animation.setFillAfter(true);
            indicator.startAnimation(animation);
        }
    }
    
    @Override
    public int getItemCount() {
        return menuItems.size();
    }
    
    // ViewHolder for main menu items
    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView menuIcon;
        TextView menuText;
        ImageView expandIndicator;
        
        MenuViewHolder(View itemView) {
            super(itemView);
            menuIcon = itemView.findViewById(R.id.menu_icon);
            menuText = itemView.findViewById(R.id.menu_text);
            expandIndicator = itemView.findViewById(R.id.expand_indicator);
        }
    }
    
    // ViewHolder for sub menu items
    static class SubMenuViewHolder extends RecyclerView.ViewHolder {
        ImageView submenuIcon;
        TextView submenuText;
        TextView submenuBadge;
        
        SubMenuViewHolder(View itemView) {
            super(itemView);
            submenuIcon = itemView.findViewById(R.id.submenu_icon);
            submenuText = itemView.findViewById(R.id.submenu_text);
            submenuBadge = itemView.findViewById(R.id.submenu_badge);
        }
    }
}
