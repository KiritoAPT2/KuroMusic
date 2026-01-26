package com.kuromusic.kizzy.gateway.entities

import com.kuromusic.kizzy.gateway.entities.op.OpCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Payload(
    @SerialName("t")
    val t: String? = null,
    @SerialName("s")
    val s: Int? = null,
    @SerialName("op")
    val op: OpCode? = null,
    @SerialName("d")
    val d: JsonElement? = null,
)