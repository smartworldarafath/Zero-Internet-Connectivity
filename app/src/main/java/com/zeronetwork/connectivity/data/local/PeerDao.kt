package com.zeronetwork.connectivity.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zeronetwork.connectivity.data.model.Peer
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY isOnline DESC, lastSeen DESC")
    fun getAllPeers(): Flow<List<Peer>>

    @Query("SELECT * FROM peers WHERE isOnline = 1")
    fun getOnlinePeers(): Flow<List<Peer>>

    @Query("SELECT * FROM peers WHERE ipAddress = :ipAddress LIMIT 1")
    suspend fun getPeerByIp(ipAddress: String): Peer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: Peer)

    @Update
    suspend fun updatePeer(peer: Peer)

    @Query("UPDATE peers SET isOnline = 0 WHERE lastSeen < :cutoffTime")
    suspend fun markStalePeersOffline(cutoffTime: Long)

    @Query("UPDATE peers SET isTyping = :isTyping WHERE ipAddress = :ipAddress")
    suspend fun setPeerTyping(ipAddress: String, isTyping: Boolean)

    @Query("DELETE FROM peers")
    suspend fun clearAllPeers()
}
