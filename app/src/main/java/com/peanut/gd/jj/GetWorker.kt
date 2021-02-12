package com.peanut.gd.jj

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.peanut.gd.jj.room.FundDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.ArrayList

class GetWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        try {
            val fundDatabase =
                Room.databaseBuilder(context, FundDatabase::class.java, "funds").build()
            val fundDao = fundDatabase.getFundDao()
            val funds = fundDao.getAllFunds()
            if (funds.isEmpty()) {
                sendNotification(arrayListOf(true to "funds is empty!"), context)
                return Result.failure()
            }
            val client = OkHttpClient()
            val ps = ArrayList<Pair<Boolean, String>>()
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
                ps.add(gszzl.startsWith("-").not() to "${fund.id}|$name|$gszzl%|${fund.fundN}")
            }
            sendNotification(ps, context)
            return Result.success()
        } catch (e: Exception) {
            sendNotification(arrayListOf(true to (e.localizedMessage ?: "unknown error")), context)
            return Result.failure()
        }
    }

    private fun sendNotification(ps: ArrayList<Pair<Boolean, String>>, context: Context) {
        if (ps.isNullOrEmpty())
            return
        val notificationManager =
            context.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                "com.peanut.gd.jj",
                "基金跌涨",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        // Get the layouts to use in the custom notification
        val notificationLayout = RemoteViews(context.packageName, R.layout.activity_main)
        notificationLayout.removeAllViews(R.id.panel)
        for (p in ps) {
            val jjViews= RemoteViews(context.packageName, R.layout.jj_item_layout)
            if (p.first) {
                jjViews.setTextColor(R.id.jj_est, Color.RED)
            } else {
                jjViews.setTextColor(R.id.jj_est, Color.parseColor("#11C436"))
            }
            val args = p.second.split("|")
            jjViews.setTextViewText(R.id.jj_code,args[0])
            jjViews.setTextViewText(R.id.jj_name,args[1])
            jjViews.setTextViewText(R.id.jj_est,if (args[2].startsWith("-").not()) "+${args[2]}" else args[2])
            if (args[3].toInt() != 0){
                jjViews.setTextViewText(R.id.jj_mount,"持有￥"+args[3])
                notificationLayout.addView(R.id.panel,jjViews)
            }
        }
        // Apply the layouts to the notification
        val customNotification = NotificationCompat.Builder(context, "com.peanut.gd.jj")
            .setSmallIcon(R.drawable.ic_image2vector)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setShowWhen(true)
            .build()
        notificationManager.notify((Math.random() * 10000).toInt(), customNotification)
    }

}