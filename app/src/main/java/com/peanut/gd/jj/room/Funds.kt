package com.peanut.gd.jj.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Funds (@PrimaryKey var id: String, var name: String, @Deprecated("use fundN")var fund: String = "", var fundN:Int = 0)