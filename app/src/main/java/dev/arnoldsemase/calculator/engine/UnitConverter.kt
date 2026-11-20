package dev.arnoldsemase.calculator.engine

/** A unit that converts to and from its category's base unit (metre, kilogram, °C, m/s, m², litre). */
class ConvUnit(
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

class UnitCategory(
    val name: String,
    val units: List<ConvUnit>,
    val defaultFrom: Int,
    val defaultTo: Int,
)

private fun linear(name: String, symbol: String, factor: Double) =
    ConvUnit(name, symbol, { it * factor }, { it / factor })

object UnitConverter {

    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            "Length",
            listOf(
                linear("Millimetre", "mm", 0.001),
                linear("Centimetre", "cm", 0.01),
                linear("Metre", "m", 1.0),
                linear("Kilometre", "km", 1000.0),
                linear("Inch", "in", 0.0254),
                linear("Foot", "ft", 0.3048),
                linear("Yard", "yd", 0.9144),
                linear("Mile", "mi", 1609.344),
            ),
            defaultFrom = 3, defaultTo = 7,
        ),
        UnitCategory(
            "Mass",
            listOf(
                linear("Milligram", "mg", 0.000001),
                linear("Gram", "g", 0.001),
                linear("Kilogram", "kg", 1.0),
                linear("Tonne", "t", 1000.0),
                linear("Ounce", "oz", 0.028349523125),
                linear("Pound", "lb", 0.45359237),
                linear("Stone", "st", 6.35029318),
            ),
            defaultFrom = 2, defaultTo = 5,
        ),
        UnitCategory(
            "Temp",
            listOf(
                ConvUnit("Celsius", "°C", { it }, { it }),
                ConvUnit("Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
                ConvUnit("Kelvin", "K", { it - 273.15 }, { it + 273.15 }),
            ),
            defaultFrom = 0, defaultTo = 1,
        ),
        UnitCategory(
            "Speed",
            listOf(
                linear("Metre / second", "m/s", 1.0),
                linear("Kilometre / hour", "km/h", 1.0 / 3.6),
                linear("Mile / hour", "mph", 0.44704),
                linear("Knot", "kn", 1852.0 / 3600.0),
                linear("Foot / second", "ft/s", 0.3048),
            ),
            defaultFrom = 1, defaultTo = 2,
        ),
        UnitCategory(
            "Area",
            listOf(
                linear("Square millimetre", "mm²", 0.000001),
                linear("Square centimetre", "cm²", 0.0001),
                linear("Square metre", "m²", 1.0),
                linear("Hectare", "ha", 10_000.0),
                linear("Square kilometre", "km²", 1_000_000.0),
                linear("Square inch", "in²", 0.00064516),
                linear("Square foot", "ft²", 0.09290304),
                linear("Acre", "ac", 4046.8564224),
            ),
            defaultFrom = 2, defaultTo = 6,
        ),
        UnitCategory(
            "Volume",
            listOf(
                linear("Millilitre", "mL", 0.001),
                linear("Litre", "L", 1.0),
                linear("Cubic metre", "m³", 1000.0),
                linear("Teaspoon (US)", "tsp", 0.00492892159375),
                linear("Tablespoon (US)", "tbsp", 0.01478676478125),
                linear("Fluid ounce (US)", "fl oz", 0.0295735295625),
                linear("Cup (US)", "cup", 0.2365882365),
                linear("Gallon (US)", "gal", 3.785411784),
            ),
            defaultFrom = 1, defaultTo = 7,
        ),
    )

    fun convert(value: Double, from: ConvUnit, to: ConvUnit): Double = to.fromBase(from.toBase(value))
}
