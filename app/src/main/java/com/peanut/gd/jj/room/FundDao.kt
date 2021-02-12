package com.peanut.gd.jj.room

import androidx.room.*

@Dao
interface FundDao {

    @Insert
    fun insertFunds(funds:Funds)

    @Delete
    fun deleteFunds(funds: Funds)

    @Update
    fun setFundMount(funds: Funds)

    @Query("SELECT * FROM Funds ORDER BY fundN DESC")
    fun getAllFunds():List<Funds>

    @Query("SELECT * FROM Funds WHERE id = :code;")
    fun getFundById(code:String):Funds
}