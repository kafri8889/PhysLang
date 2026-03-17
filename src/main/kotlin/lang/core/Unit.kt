package lang.core;

enum class Unit(val symbol: String, val scale: Double) {

    // Mass (base: kg)
    Kilogram("kg", 1.0),
    Gram("g", 0.001),
    Milligram("mg", 1e-6),
    Microgram("µg", 1e-9),
    Tonne("t", 1000.0),

    Ounce("oz", 0.028349523125),
    Pound("lb", 0.45359237),
    Stone("st", 6.35029318),

    // Length (base: m)
    Meter("m", 1.0),
    Kilometer("km", 1000.0),
    Centimeter("cm", 0.01),
    Millimeter("mm", 0.001),
    Micrometer("µm", 1e-6),
    Nanometer("nm", 1e-9),

    Inch("in", 0.0254),
    Foot("ft", 0.3048),
    Yard("yd", 0.9144),
    Mile("mi", 1609.344);

    companion object {
        fun fromSymbol(symbol: String): Unit? {
            return entries.find { it.symbol == symbol }
        }
    }
}