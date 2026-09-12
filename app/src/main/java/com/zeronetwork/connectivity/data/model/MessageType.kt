package com.zeronetwork.connectivity.data.model

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    SYSTEM,
    CALL_LOG,
    TYPING
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

enum class CallState {
    IDLE,
    OUTGOING_CALLING,
    INCOMING_RINGING,
    CONNECTED,
    ENDED,
    BUSY,
    REJECTED
}
