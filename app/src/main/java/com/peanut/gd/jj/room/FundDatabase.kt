package com.peanut.gd.jj.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Funds::class],version = 3,exportSchema = false)
abstract class FundDatabase:RoomDatabase() {
    abstract fun getFundDao():FundDao
}