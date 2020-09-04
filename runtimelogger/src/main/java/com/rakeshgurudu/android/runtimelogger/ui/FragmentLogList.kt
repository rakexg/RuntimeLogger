package com.rakeshgurudu.android.runtimelogger.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rakeshgurudu.android.runtimelogger.R
import com.rakeshgurudu.android.runtimelogger.core.LogModel
import com.rakeshgurudu.android.runtimelogger.core.RuntimeLogger
import java.io.File

class FragmentLogList : Fragment() {

    private lateinit var msgTV: TextView
    private lateinit var rootView: View
    private lateinit var listAdapter: RVAdapter
    private lateinit var mainRV: RecyclerView

    companion object {
        @JvmStatic
        fun newInstance(): FragmentLogList {
            return FragmentLogList().apply {
                arguments = Bundle().apply {
                    //putInt(ARG_VIEW_TYPE, viewType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //viewType = it.getInt(ARG_VIEW_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_log_list, container, false)
        mainRV = rootView.findViewById(R.id.recyclerview)
        msgTV = rootView.findViewById(R.id.msgTV)
        setupRV()
        fetchData()
        return rootView
    }

    private fun setupRV() {
        val rvLayoutManager: RecyclerView.LayoutManager?
        rvLayoutManager = LinearLayoutManager(activity)
        val itemClickListener = object : RVAdapter.OnListItemClick {
            override fun onItemClick(view: View, item: Any, position: Int) {
                when (view.id) {
                    R.id.delete -> {
                        val log = item as LogModel
                        val file = File(log.filePath)
                        if (file.exists()) {
                            if (file.delete()) {
                                fetchData()
                                Toast.makeText(
                                    view.context,
                                    String.format(
                                        getString(R.string.file_delete_success),
                                        log.fileName
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    view.context,
                                    String.format(
                                        getString(R.string.file_delete_fail),
                                        log.fileName
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {

                            Toast.makeText(
                                view.context,
                                String.format(
                                    getString(R.string.file_not_exists),
                                    log.fileName,
                                    file.absolutePath
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    R.id.share -> {
                        val log = item as LogModel
                        val file = File(log.filePath)
                        if (file.exists()) {
                            val shareIntent = Intent()
                            try {
                                val fileUri =
                                    FileProvider.getUriForFile(
                                        view.context,
                                        "${context?.packageName}.RuntimeLoggerFileProvider",
                                        File(file.absolutePath)
                                    )
                                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    view.context,
                                    getString(R.string.attach_file_failed),
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }
                            shareIntent.action = Intent.ACTION_SEND
                            shareIntent.type = "text/plain"
                            startActivity(Intent.createChooser(shareIntent, null))
                            /*Toast.makeText(
                                view.context,
                                "Share ${(item as LogModel).fileName}",
                                Toast.LENGTH_SHORT
                            ).show()*/
                        }
                    }
                }
            }
        }
        listAdapter = RVAdapter(itemClickListener)
        with(mainRV) {
            layoutManager = rvLayoutManager
            adapter = listAdapter
            //addOnScrollListener(infiniteScrollListener)
            setHasFixedSize(true)
        }
    }

    private fun fetchData() {
        mainRV.visibility = View.VISIBLE
        val folder = File(RuntimeLogger.logDirectoryPath)
        if (!folder.exists()) {
            return
        }
        val listOfFiles: Array<File>? = folder.listFiles()
        if (listOfFiles != null) {
            val dataList = ArrayList<LogModel>()
            for (i in listOfFiles.indices) {
                val file = listOfFiles[i]
                if (file.isFile) {
                    dataList.add(
                        LogModel(
                            i,
                            file.name,
                            file.absolutePath,
                            file.sizeInMb,
                            file.lastModified()
                        )
                    )
                }
            }
            dataList.sortByDescending { it.lastModified }
            listAdapter.removeAllItems()
            listAdapter.addItems(dataList)
            if (dataList.isEmpty()) {
                msgTV.visibility = View.VISIBLE
                msgTV.text = getString(R.string.empty_logs)
            }
        }

    }

    fun deleteAllFiles() {
        val folder = File(RuntimeLogger.logDirectoryPath)
        if (!folder.exists()) {
            Toast.makeText(
                context,
                getString(R.string.dir_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val listOfFiles: Array<File>? = folder.listFiles()
        if (listOfFiles != null) {
            for (file in listOfFiles) {
                if (!file.isDirectory) {
                    file.delete()
                }
            }
        }
        Toast.makeText(context, getString(R.string.all_files_deleted), Toast.LENGTH_SHORT).show()
        fetchData()
    }

    //Extension functions to get file size in KB, MB, GB and TB
    private val File.size get() = if (!exists()) 0.0 else length().toDouble()
    private val File.sizeInKb get() = size / 1024
    private val File.sizeInMb get() = sizeInKb / 1024
    //private val File.sizeInGb get() = sizeInMb / 1024
    //private val File.sizeInTb get() = sizeInGb / 1024

}
