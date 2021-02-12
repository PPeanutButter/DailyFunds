package com.peanut.gd.jj

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.room.Room
import com.peanut.gd.jj.room.FundDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.absoluteValue

class JJWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        //在调用update之前做个预处理
        val action = intent.action
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == action) {
            if (intent.extras == null) {
                //这里是由shell启动的，手动加参数:ids
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, JJWidgetProvider::class.java))
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
                R.layout.jj_widget_layout
            )
            thread {
                val fundDatabase =
                    Room.databaseBuilder(context, FundDatabase::class.java, "funds").build()
                val fundDao = fundDatabase.getFundDao()
                val funds = fundDao.getAllFunds()
                if (funds.isEmpty()) {
                    return@thread
                }
                val client = OkHttpClient()
                val ps = ArrayList<Pair<Boolean, String>>()
                var gztime = ""
                for (fund in funds) {
                    val request = Request.Builder()
                        .url("https://fundgz.1234567.com.cn/js/${fund.id}.js")
                        .get()
                        .build()
                    val response = client.newCall(request).execute().body!!.string()
                    val json = JSONObject(
                        response.substring(
                            response.indexOf("(") + 1,
                            response.lastIndexOf(")")
                        )
                    )
                    val name = json.getString("name")
                    val gszzl = json.getString("gszzl")
                    gztime = json.getString("gztime")
                    ps.add(gszzl.startsWith("-").not() to "${fund.id}|$name|$gszzl|${fund.fundN}")
                }
                views.removeAllViews(R.id.app_widget_panel)
                ps.forEach { p->
                    val jjViews= RemoteViews(context.packageName, R.layout.jj_item_layout)
                    if (p.first) {
                        jjViews.setTextColor(R.id.jj_est,Color.RED)
                    } else {
                        jjViews.setTextColor(R.id.jj_est,Color.parseColor("#11C436"))
                    }
                    val args = p.second.split("|")
                    jjViews.setTextViewText(R.id.jj_code,args[0])
                    jjViews.setTextViewText(R.id.jj_name,args[1])
                    jjViews.setTextViewText(R.id.jj_est,if (args[2].startsWith("-").not()) "+${args[2]}%" else "${args[2]}%")
                    //添加持有金额
                    if (args[3].toInt() > 0){
                        val holdView = RemoteViews(context.packageName, R.layout.jj_status_hold)
                        holdView.setTextViewText(R.id.jj_mount,"持有￥"+args[3])
                        jjViews.addView(R.id.jj_status_panel,holdView)
                    }
                    //添加估值趋势
                    if (args[2].toDouble().absoluteValue > 2.9){
                        val trendView = if (p.first) RemoteViews(context.packageName, R.layout.up_high_trend) else RemoteViews(context.packageName, R.layout.down_high_trend)
                        jjViews.addView(R.id.jj_status_panel,trendView)
                    }
                    views.addView(R.id.app_widget_panel,jjViews)
                }
                views.setTextViewText(R.id.app_widget_update_time,"数据:$gztime|界面:${SimpleDateFormat("HH:mm",
                    Locale.CHINA) .format(Date())}")
                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
                fundDatabase.close()
            }
        }
    }
}