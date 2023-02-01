package com.example.vplayer.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vplayer.FolderAcivity
import com.example.vplayer.databinding.FolderViewBinding
import com.example.vplayer.dataclass.Folder

class FolderAdapter(private val context: Context, var folderList: ArrayList<Folder>) : RecyclerView.Adapter<FolderAdapter.MyHolder>(){
    class MyHolder(binding : FolderViewBinding): RecyclerView.ViewHolder(binding.root) {

        val folderName = binding.folderNameFV
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(FolderViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.folderName.text = folderList[position].folderName
        holder.root.setOnClickListener {
            val intent = Intent(context,FolderAcivity::class.java)
            intent.putExtra("position",position)
            ContextCompat.startActivity(context,intent,null)
        }

    }
}