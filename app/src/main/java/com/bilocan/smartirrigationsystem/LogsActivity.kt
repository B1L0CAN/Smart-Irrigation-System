package com.bilocan.smartirrigationsystem

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import java.util.concurrent.TimeUnit

class LogsActivity : AppCompatActivity() {

    private lateinit var rvLogs: RecyclerView
    private lateinit var btnClearLogs: Button
    private lateinit var btnBack: Button
    private lateinit var tvEmptyLogs: TextView
    private lateinit var emptyLogsView: ConstraintLayout
    
    // Log kayıtları
    private val logsList = mutableListOf<LogEntry>()
    private lateinit var logsAdapter: LogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        // LogManager'ı initialize et
        LogManager.initialize(this)

        // UI elemanlarını bağla
        rvLogs = findViewById(R.id.rvLogs)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        btnBack = findViewById(R.id.btnBack)
        tvEmptyLogs = findViewById(R.id.tvEmptyLogs)
        emptyLogsView = findViewById(R.id.emptyLogsView)

        // RecyclerView ayarları
        rvLogs.layoutManager = LinearLayoutManager(this)
        rvLogs.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        logsAdapter = LogsAdapter(logsList)
        rvLogs.adapter = logsAdapter

        // Kayıtlı logları yükle
        loadLogs()

        // Buton click olayları
        btnClearLogs.setOnClickListener {
            showClearLogsConfirmationDialog()
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Boş görünüm kontrolü
        updateEmptyView()
    }
    
    // Log temizleme onay diyaloğu
    private fun showClearLogsConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clear_logs, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            
        val btnOnayla = dialogView.findViewById<Button>(R.id.btnOnayla)
        val btnIptal = dialogView.findViewById<Button>(R.id.btnIptal)
        
        btnOnayla.setOnClickListener {
            logsList.clear()
            LogManager.clearLogs(this)
            logsAdapter.notifyDataSetChanged()
            updateEmptyView()
            alertDialog.dismiss()
        }
        
        btnIptal.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }

    // Logları yükle
    private fun loadLogs() {
        logsList.clear()
        logsList.addAll(LogManager.getLogs())
        logsAdapter.notifyDataSetChanged()
        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (logsList.isEmpty()) {
            tvEmptyLogs.visibility = View.VISIBLE
            rvLogs.visibility = View.GONE
            emptyLogsView.visibility = View.VISIBLE
            btnClearLogs.isEnabled = false
        } else {
            tvEmptyLogs.visibility = View.GONE
            rvLogs.visibility = View.VISIBLE
            emptyLogsView.visibility = View.GONE
            btnClearLogs.isEnabled = true
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Aktivite tekrar açıldığında logları yenile
        loadLogs()
    }

    override fun onPause() {
        super.onPause()
        // Aktivite durduğunda logları kaydet
        LogManager.saveLogsToStorage(this)
    }
}

// Log türleri
enum class LogType {
    INFO, WARNING, ERROR, SEPARATOR
}

// Log kaydı modeli
data class LogEntry(
    val message: String,
    val timestamp: Long,
    val type: LogType,
    val details: String = ""
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))
        return dateFormat.format(Date(timestamp))
    }
    
    fun getFormattedHour(): String {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }
}

// RecyclerView için adapter
class LogsAdapter(private val logsList: List<LogEntry>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_LOG = 0
        private const val VIEW_TYPE_SEPARATOR = 1
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvLogDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvLogTime)
        val tvMessage: TextView = itemView.findViewById(R.id.tvLogMessage)
        val tvDetails: TextView = itemView.findViewById(R.id.tvLogDetails)
    }
    
    class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (logsList[position].type == LogType.SEPARATOR) {
            VIEW_TYPE_SEPARATOR
        } else {
            VIEW_TYPE_LOG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SEPARATOR) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log_separator, parent, false)
            SeparatorViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log, parent, false)
            LogViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val log = logsList[position]
        
        if (holder is LogViewHolder) {
            // Tarih ve saat bilgilerini ayarla
            holder.tvDate.text = log.getFormattedDate()
            holder.tvTime.text = log.getFormattedHour()
            
            // Mesaj ve renk ayarla
            holder.tvMessage.text = log.message
            val messageColor = when (log.type) {
                LogType.INFO -> Color.BLACK
                LogType.WARNING -> Color.parseColor("#FF9800") // Turuncu
                LogType.ERROR -> Color.RED
                LogType.SEPARATOR -> Color.GRAY  // Bu durumda kullanılmayacak ama gerekli
            }
            holder.tvMessage.setTextColor(messageColor)
            
            // Detay bilgisi
            if (log.details.isNotEmpty()) {
                holder.tvDetails.visibility = View.VISIBLE
                holder.tvDetails.text = log.details
            } else {
                holder.tvDetails.visibility = View.GONE
            }
        }
        // SeparatorViewHolder için özel bir ayarlama gerekmiyor
    }

    override fun getItemCount() = logsList.size
}

// Log yöneticisi sınıfı
object LogManager {
    private const val PREFS_NAME = "SmartIrrigationLogs"
    private const val LOGS_KEY = "system_logs"
    
    private var logs = mutableListOf<LogEntry>()
    private lateinit var context: Context
    private val gson = Gson()
    private var initialized = false
    private var lastAction = "NONE"  // "OPEN" veya "CLOSE" olabilir
    
    fun initialize(context: Context) {
        if (!initialized) {
            this.context = context.applicationContext
            loadLogsFromStorage()
            initialized = true
            
            // Son eylem türünü belirle
            if (logs.isNotEmpty() && logs[0].type != LogType.SEPARATOR) {
                lastAction = if (logs[0].message.contains("açıldı")) "OPEN" else "CLOSE"
            }
        }
    }
    
    fun addLog(log: LogEntry) {
        if (log.type == LogType.SEPARATOR) {
            // Ayraç eklenirken liste başına ekle
            logs.add(0, log)
        } else {
            // Normal log eklenirken
            logs.add(0, log)
            
            // Son eylemi güncelle
            lastAction = if (log.message.contains("açıldı")) "OPEN" else "CLOSE"
        }
        
        saveLogsToStorage()
    }
    
    fun addLogWithSeparator(timestamp: Long) {
        // Önce ayraç ekle
        val separator = LogEntry(
            message = "",
            timestamp = timestamp,
            type = LogType.SEPARATOR,
            details = ""
        )
        logs.add(0, separator)
        saveLogsToStorage()
    }
    
    fun logValveOpen(message: String, timestamp: Long, details: String) {
        addLog(LogEntry(
            message = message,
            timestamp = timestamp,
            type = LogType.INFO,
            details = details
        ))
    }
    
    // Son eylemin kapatma olup olmadığını kontrol et
    fun isLastActionClose(): Boolean {
        return lastAction == "CLOSE" || lastAction == "NONE"
    }
    
    fun logWaterFlow(flowRate: Float, timestamp: Long, duration: Long) {
        val details = "Su akış hızı: $flowRate L/dk, Süre: ${formatDuration(duration)}"
        addLog(LogEntry(
            message = "Su akışı kaydedildi",
            timestamp = timestamp,
            type = LogType.INFO,
            details = details
        ))
    }
    
    fun getLogs(): List<LogEntry> {
        return logs
    }
    
    fun clearLogs(context: Context) {
        logs.clear()
        saveLogsToStorage()
    }
    
    fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        
        return if (hours > 0) {
            String.format("%d saat %d dakika %d saniye", hours, minutes, seconds)
        } else if (minutes > 0) {
            String.format("%d dakika %d saniye", minutes, seconds)
        } else {
            String.format("%d saniye", seconds)
        }
    }
    
    fun saveLogsToStorage(context: Context) {
        this.context = context.applicationContext
        saveLogsToStorage()
    }
    
    private fun saveLogsToStorage() {
        if (!::context.isInitialized) return
        
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logsJson = gson.toJson(logs)
        
        sharedPrefs.edit().putString(LOGS_KEY, logsJson).apply()
    }
    
    private fun loadLogsFromStorage() {
        if (!::context.isInitialized) return
        
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString(LOGS_KEY, null)
        
        if (logsJson != null) {
            val type = object : TypeToken<MutableList<LogEntry>>() {}.type
            logs = gson.fromJson(logsJson, type)
        }
    }
} 