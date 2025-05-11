package com.bilocan.smartirrigationsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bilocan.smartirrigationsystem.model.ForecastDay

class ForecastAdapter(private val forecastList: List<ForecastDay>) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {
    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        val ivForecastIcon: ImageView = itemView.findViewById(R.id.ivForecastIcon)
        val tvForecastTemp: TextView = itemView.findViewById(R.id.tvForecastTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forecast_day, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val item = forecastList[position]
        holder.tvDayName.text = item.dayName
        holder.ivForecastIcon.setImageResource(item.iconResId)
        holder.tvForecastTemp.text = "${item.maxTemp}° ${item.minTemp}°"
    }

    override fun getItemCount(): Int = forecastList.size
} 