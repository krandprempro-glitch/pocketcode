package com.termux.app.configuration.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.termux.app.models.DirectoryBookmark

/**
 * 路径收藏下拉适配器
 * 用于在Spinner中显示收藏的目录路径
 */
class PathBookmarkSpinnerAdapter(context: Context) : 
    ArrayAdapter<DirectoryBookmark>(context, android.R.layout.simple_spinner_item) {
    
    private var bookmarks: List<DirectoryBookmark> = emptyList()
    
    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view as TextView
        
        val bookmark = getItem(position)
        bookmark?.let {
            val displayText = "${it.displayName}\n${it.fullPath}"
            textView.text = displayText
        }
        
        return view
    }
    
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val textView = view as TextView
        
        val bookmark = getItem(position)
        bookmark?.let {
            textView.text = "${it.displayName} - ${it.fullPath}"
        }
        
        return view
    }
    
    fun setBookmarks(bookmarks: List<DirectoryBookmark>) {
        this.bookmarks = bookmarks
        clear()
        addAll(bookmarks)
        notifyDataSetChanged()
    }
    
    fun getBookmarks(): List<DirectoryBookmark> {
        return bookmarks
    }
}