package com.game.gamepad.config

/**
 * GameButton 的数据类 存储所有button的信息
 */
data class ConfigBean(
    val buttons: List<Button>,
    val desc: String
)

data class Button(
    val height: Int,
    val key: String,
    val text: String,
    val type: Int,
    val width: Int,
    val x: Float,
    val y: Float,
    val r: Int
)