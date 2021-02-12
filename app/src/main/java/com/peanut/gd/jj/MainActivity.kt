package com.peanut.gd.jj

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.peanut.gd.jj.room.FundDao
import com.peanut.gd.jj.room.FundDatabase
import com.peanut.gd.jj.room.Funds
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : CMDActivity() {
    private lateinit var fundDatabase: FundDatabase
    lateinit var fundDao: FundDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fundDatabase =
            Room.databaseBuilder(this, FundDatabase::class.java, "funds").addMigrations(object :
                Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE Funds ADD COLUMN fund TEXT NOT NULL DEFAULT '';")
                }
            }, object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE Funds ADD COLUMN fundN INTEGER NOT NULL DEFAULT 0;")
                }
            }).build()
        fundDao = fundDatabase.getFundDao()
        registerInputListener { populateCMD(it) }
        printHelp()
    }

    private fun populateCMD(cmd: String) {
        printV("$ $cmd")
        val commands = cmd.split(" ")
        try {
            when (commands[0].toLowerCase(Locale.CHINA)) {
                "a", "add" -> c(commands[1])
                "ls", "lists" -> ls()
                "d", "delete" -> d(commands[1])
                "s", "set" -> s(commands[1], commands[2].toInt())
                "u", "update" -> u()
                else -> printE("Unknown Command:${commands[0]}")
            }
        } catch (e: Exception) {
            printE("Error->" + e.localizedMessage)
        }
    }

    private fun printHelp() {
        printV("")
        printV("Usage:")
        printV("    <command> <parameters>")
        printV("")
        printV("Commands:")
        printV("    a,add           添加一个基金代码.")
        printV("    d,delete        删除一个基金代码.")
        printV("    ls,lists        列出所有基金.")
        printV("    s,set           修改持仓金额(正加仓负减仓0清仓).")
        printV("    u,update        更新桌面小部件.")
        printV("")
    }

    private fun c(fundID: String) {
        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://fundgz.1234567.com.cn/js/$fundID.js")
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
                val fund = Funds(fundID, name)
                fundDao.insertFunds(fund)
                Handler(this@MainActivity.mainLooper).post {
                    printI("已添加 $fundID|$name")
                    u()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun u() {
        sendBroadcast(Intent(this, JJWidgetProvider::class.java).also {
            it.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        })
    }

    private fun ls() {
        thread { for (fund in fundDao.getAllFunds()) ui { printI(fund.id + "," + fund.name + "," + fund.fundN) } }
    }

    private fun d(fundID: String) {
        thread {
            fundDao.deleteFunds(Funds(fundID, ""))
            ui { u() }
        }
    }

    private fun s(fundID: String, fundMount: Int) {
        thread {
            val fund = fundDao.getFundById(fundID)
            if (fundMount == 0) {
                fund.fundN = 0
                printW("${fund.name} 已清仓")
            } else {
                fund.fundN = fund.fundN + fundMount
                if (fundMount > 0) printW("${fund.name} 加仓 $fundMount")
                else printW("${fund.name} 减仓 ${abs(fundMount)}")
            }
            fundDao.setFundMount(fund)
            ui { u() }
        }
    }

    private fun ui(func: () -> Unit) {
        Handler(this.mainLooper).post {
            func.invoke()
        }
    }

}