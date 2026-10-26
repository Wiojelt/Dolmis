package org.dolmis.diyarbakir

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DolmisWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs=context.getSharedPreferences("dolmis", Context.MODE_PRIVATE)
        update(context, manager, ids, prefs.getString("route", "Z7")?.let { runCatching { Route(routeFromJson(org.json.JSONObject(it)).code, routeFromJson(org.json.JSONObject(it)).line, "").displayCode }.getOrDefault("Z7") } ?: "Z7", prefs.getInt("widgetBusCount",0), prefs.getLong("widgetUpdatedAt",0L))
    }
    companion object {
        private val clock=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Istanbul"))
        fun updateAll(context: Context, route: String, count: Int, updated: Long?) {
            val prefs=context.getSharedPreferences("dolmis", Context.MODE_PRIVATE)
            prefs.edit().putString("widgetRoute",route).putInt("widgetBusCount",count).putLong("widgetUpdatedAt",updated?:0L).apply()
            val manager=AppWidgetManager.getInstance(context)
            val ids=manager.getAppWidgetIds(ComponentName(context, DolmisWidgetProvider::class.java))
            update(context, manager, ids, route, count, updated?:0L)
        }
        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray, route: String, count: Int, updated: Long) {
            ids.forEach { id ->
                val views=RemoteViews(context.packageName, R.layout.widget_dolmis)
                views.setTextViewText(R.id.widget_route, "Dolmiş · $route")
                views.setTextViewText(R.id.widget_count, if(count>0) "$count otobüs canlı" else "Canlı veri bekleniyor")
                views.setTextViewText(R.id.widget_time, if(updated>0) "Son sorgu ${clock.format(Instant.ofEpochMilli(updated))}" else "Uygulamayı açıp yenile")
                views.setOnClickPendingIntent(R.id.widget_root, android.app.PendingIntent.getActivity(context,0,android.content.Intent(context,MainActivity::class.java),android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id,views)
            }
        }
    }
}
