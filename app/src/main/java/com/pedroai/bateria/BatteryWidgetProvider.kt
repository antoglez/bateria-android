package com.pedroai.bateria

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.widget.RemoteViews

class BatteryWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateAllWidgets(context: Context, batteryPct: Int, isCharging: Boolean) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_battery)

                views.setTextViewText(R.id.battery_percentage_text, if (batteryPct != -1) "$batteryPct%" else "--%")
                
                val statusText = if (isCharging) "Cargando ⚡" else ""
                views.setTextViewText(R.id.battery_status_text, statusText)

                val colorInt = when {
                    batteryPct > 50 -> Color.parseColor("#34C759") // Verde
                    batteryPct > 15 -> Color.parseColor("#FFCC00") // Amarillo
                    batteryPct != -1 -> Color.parseColor("#FF3B30") // Rojo
                    else -> Color.WHITE
                }
                views.setTextColor(R.id.battery_percentage_text, colorInt)

                // Seleccionar icono estático super seguro 100% compatible
                val iconRes = when {
                    isCharging -> R.drawable.ic_battery_charge
                    batteryPct > 80 -> R.drawable.ic_battery_100
                    batteryPct > 50 -> R.drawable.ic_battery_80
                    batteryPct > 15 -> R.drawable.ic_battery_50
                    else -> R.drawable.ic_battery_15
                }
                views.setImageViewResource(R.id.battery_icon, iconRes)

                // Clic en el porcentaje o fondo para abrir app
                val appIntent = Intent(context, MainActivity::class.java)
                val pendingAppIntent = PendingIntent.getActivity(
                    context,
                    1,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.battery_percentage_text, pendingAppIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val serviceIntent = Intent(context, BatteryService::class.java)
        context.stopService(serviceIntent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Obtener estado inicial estático por si el servicio no está corriendo
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else -1
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        updateAllWidgets(context, batteryPct, isCharging)
    }
}
