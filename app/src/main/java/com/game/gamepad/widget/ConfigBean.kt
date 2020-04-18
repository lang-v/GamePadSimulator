package com.game.gamepad.widget


data class ConfigBean(
    val buttons: List<Button>,
    val desc: String,
    val name: String
)

data class Button(
    val height: Int,
    val key: String,
    val width: Int,
    val x: Float,
    val y: Float,
    val r: Int
)