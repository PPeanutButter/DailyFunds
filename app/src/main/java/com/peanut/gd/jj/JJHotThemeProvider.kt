package com.peanut.gd.jj

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.concurrent.thread

class JJHotThemeProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        //在调用update之前做个预处理
        val action = intent.action
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == action) {
            if (intent.extras == null) {
                //这里是由shell启动的，手动加参数:ids
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, JJHotThemeProvider::class.java))
                )
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_hot_realtime
            )
            thread {
                //首先读取list
                val request = Request.Builder()
                    .url("http://api.fund.eastmoney.com/ztjj/GetZTJJList?tt=0&dt=syl&st=ZDF&_=${System.currentTimeMillis()}")
                    .get()
                    .addHeader("Referer","http://fund.eastmoney.com/")
                    .build()
                val response = OkHttpClient().newCall(request).execute().body!!.string()
                val topBody = JSONObject(response).getJSONArray("Data").getJSONObject(0)
                val (name,code) = Pair(topBody.getString("BKName"),topBody.getString("BKCode"))
                //再读取实时数据
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}