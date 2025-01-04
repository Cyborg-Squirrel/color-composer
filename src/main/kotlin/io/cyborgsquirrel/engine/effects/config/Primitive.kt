package io.cyborgsquirrel.engine.effects.config

enum class Primitive {
    Float,
    Integer,
    SingleColor,
    ListOfColors;

    companion object {
        fun fromString(string: String): Primitive {
            return when(string) {
                Float.name -> Float
                Integer.name -> Integer
                SingleColor.name -> SingleColor
                ListOfColors.name -> ListOfColors
                else -> throw Exception()
            }
        }
    }
}