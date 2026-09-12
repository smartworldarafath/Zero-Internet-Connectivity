package com.zeronetwork.connectivity.data.model

data class UpdateItem(
    val version: String,
    val releaseDate: String,
    val title: String,
    val changelog: List<String>,
    val isCurrent: Boolean = false
)
