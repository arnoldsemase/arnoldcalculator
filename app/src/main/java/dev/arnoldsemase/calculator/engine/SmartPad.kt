package dev.arnoldsemase.calculator.engine

import kotlin.math.pow

/** What a Smart Pad number is measured in: a currency, or a unit from [UnitConverter]. */
sealed interface PadUnit {
    data class Currency(val code: String) : PadUnit
    data class Measure(val category: UnitCategory, val unit: ConvUnit) : PadUnit
}

/** One Smart Pad result. [percent] values hold the fraction (15% is 0.15). */
data class PadValue(
    val number: Double,
    val unit: PadUnit? = null,
    val percent: Boolean = false,
)

/**
 * Smart Pad: a notepad calculator. Every line is evaluated on its own, top to bottom, with these rules:
 *  - words are ignored unless they mean something (a variable, a unit, a currency, "of", "in", "total"…)
 *  - `name = expression` defines a variable for the lines below it
 *  - `20% of 500`, `100 + 15%`, `total` (sum of the lines above), `ans` (previous result)
 *  - `5 km in miles`, `100 USD in ZAR` reuse [UnitConverter] and the cached exchange rates
 * Pure and deterministic: no clock, no network, no randomness. Anything incomplete or invalid simply
 * produces no result for that line, so a half-typed line never throws and never shows an error.
 */
object SmartPad {

    private const val MAX_LINE = 2_000
    private const val MAX_DEPTH = 64

    /** One entry per line of [text] (split on '\n'); null means "nothing to show for this line". */
    fun evaluate(text: String, rates: Map<String, Double>? = null): List<PadValue?> {
        val lines = text.replace("\r", "").split('\n')
        val vars = HashMap<String, PadValue>()
        val results = ArrayList<PadValue?>(lines.size)
        val summable = ArrayList<PadValue>()
        var previous: PadValue? = null

        for (line in lines) {
            val ctx = Ctx(vars, rates, previous) { sumOf(summable, rates) }
            val value = try {
                evalLine(line, ctx)
            } catch (e: Exception) {
                null
            } catch (e: StackOverflowError) {
                null
            }
            results.add(value)
            if (value != null) {
                previous = value
                if (!value.percent && !ctx.usedTotal) summable.add(value)
            }
        }
        return results
    }

    /** Display text for a result, using the app's number formatting. */
    fun format(value: PadValue, formatter: NumberFormatter): String {
        if (value.percent) return formatter.display(NumberFormatter.toPlain(value.number * 100.0)) + "%"
        return when (val u = value.unit) {
            is PadUnit.Currency -> formatter.display(CurrencyConverter.moneyPlain(value.number)) + " " + u.code
            is PadUnit.Measure -> formatter.display(NumberFormatter.toPlain(value.number)) + " " + u.unit.symbol
            null -> formatter.display(NumberFormatter.toPlain(value.number))
        }
    }

    // ---- Lines --------------------------------------------------------------------------------

    private class Ctx(
        val vars: MutableMap<String, PadValue>,
        val rates: Map<String, Double>?,
        val previous: PadValue?,
        val total: () -> PadValue?,
    ) {
        var usedTotal = false
    }

    private val RESERVED = setOf(
        "total", "sum", "subtotal", "ans", "prev", "previous",
        "in", "to", "into", "as", "of", "at", "x", "times", "percent",
    )

    private fun evalLine(line: String, ctx: Ctx): PadValue? {
        if (line.length > MAX_LINE) return null
        val raw = line.substringBefore("//")
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null

        var name: String? = null
        var body = raw
        val eq = raw.indexOf('=')
        if (eq > 0) {
            val lhs = raw.substring(0, eq)
            val normalized = variableName(lhs)
            if (normalized != null) {
                name = normalized
                body = raw.substring(eq + 1)
            }
        }

        val items = clean(tokenize(body, ctx.rates), ctx)
        val value = Parser(items, ctx.rates).parseAll()?.takeIf { it.number.isFinite() }
        if (name != null && value != null) ctx.vars[name] = value
        return value
    }

    /** "Monthly  Rent" → "monthly rent"; null when [lhs] is not a usable variable name. */
    private fun variableName(lhs: String): String? {
        val t = lhs.trim()
        if (t.isEmpty()) return null
        if (!(t[0].isLetter() || t[0] == '_')) return null
        if (!t.all { it.isLetterOrDigit() || it == '_' || it == ' ' }) return null
        val words = t.lowercase().split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty() || words.joinToString(" ") in RESERVED) return null
        return words.joinToString(" ")
    }

    private fun sumOf(rows: List<PadValue>, rates: Map<String, Double>?): PadValue? {
        var acc: PadValue? = null
        for (row in rows) {
            val a = acc
            if (a == null) {
                acc = row
            } else {
                try {
                    acc = Parser(emptyList(), rates).addSub(a, row, true)
                } catch (e: Bail) {
                    // incompatible units: that line is left out of the total
                }
            }
        }
        return acc
    }

    // ---- Tokenizer ----------------------------------------------------------------------------

    private sealed interface Tok {
        data class Num(val v: Double) : Tok
        data class Word(val s: String) : Tok
        data class Op(val c: Char) : Tok
        data class Cur(val code: String) : Tok
        data object LP : Tok
        data object RP : Tok
        data object Pct : Tok
        data object Eq : Tok
    }

    private fun isDigit(c: Char) = c in '0'..'9'

    private fun tokenize(src: String, rates: Map<String, Double>?): List<Tok> {
        val out = ArrayList<Tok>()
        val n = src.length
        var i = 0
        while (i < n) {
            val ch = src[i]
            when {
                ch.isWhitespace() -> i++
                // "R8500" is rand
                ch == 'R' && i + 1 < n && isDigit(src[i + 1]) && (i == 0 || !src[i - 1].isLetterOrDigit()) -> {
                    out.add(Tok.Cur("ZAR")); i++
                }
                isDigit(ch) || (ch == '.' && i + 1 < n && isDigit(src[i + 1])) -> {
                    val sb = StringBuilder()
                    var seenDot = false
                    var j = i
                    while (j < n) {
                        val c = src[j]
                        if (isDigit(c)) {
                            sb.append(c); j++
                        } else if (c == '.' && !seenDot && j + 1 < n && isDigit(src[j + 1])) {
                            if (sb.isEmpty()) sb.append('0')
                            seenDot = true; sb.append('.'); j++
                        } else if (c == ',' && !seenDot && sb.isNotEmpty() && isGroupComma(src, j)) {
                            j++ // 1,234 → 1234
                        } else {
                            break
                        }
                    }
                    val v = sb.toString().toDoubleOrNull()
                    if (v != null && v.isFinite()) out.add(Tok.Num(v))
                    i = j
                }
                ch.isLetter() || ch == '_' || ch == '°' -> {
                    var j = i
                    while (j < n && (src[j].isLetter() || src[j] == '_' || src[j] == '°' ||
                            src[j] == '²' || src[j] == '³' || (j > i && isDigit(src[j])))
                    ) j++
                    var word = src.substring(i, j).lowercase()
                    // km/h, m/s, ft/s
                    if (j + 1 < n && src[j] == '/' && src[j + 1].isLetter()) {
                        var k = j + 1
                        while (k < n && src[k].isLetter()) k++
                        val merged = word + "/" + src.substring(j + 1, k).lowercase()
                        if (merged in UNIT_ALIASES) {
                            word = merged
                            j = k
                        }
                    }
                    out.add(Tok.Word(word))
                    i = j
                }
                else -> {
                    when (ch) {
                        '+' -> out.add(Tok.Op('+'))
                        '-', '−', '–' -> out.add(Tok.Op('-'))
                        '*', '×' -> out.add(Tok.Op('*'))
                        '/', '÷' -> out.add(Tok.Op('/'))
                        '^' -> out.add(Tok.Op('^'))
                        '(' -> out.add(Tok.LP)
                        ')' -> out.add(Tok.RP)
                        '%' -> out.add(Tok.Pct)
                        '=' -> out.add(Tok.Eq)
                        '$' -> out.add(Tok.Cur("USD"))
                        '€' -> out.add(Tok.Cur("EUR"))
                        '£' -> out.add(Tok.Cur("GBP"))
                        '¥' -> out.add(Tok.Cur("JPY"))
                        '@' -> out.add(Tok.Word("at"))
                        else -> Unit // punctuation is just a separator
                    }
                    i++
                }
            }
        }
        return out
    }

    /** True for the comma in "1,234" (exactly three digits follow). */
    private fun isGroupComma(src: String, j: Int): Boolean {
        if (j + 3 >= src.length) return false
        if (!(isDigit(src[j + 1]) && isDigit(src[j + 2]) && isDigit(src[j + 3]))) return false
        return j + 4 >= src.length || !isDigit(src[j + 4])
    }

    // ---- Units and currencies ------------------------------------------------------------------

    private class Resolved(val unit: PadUnit, val words: Int)

    private val UNIT_ALIASES: Map<String, PadUnit.Measure> by lazy { buildAliases() }

    private fun buildAliases(): Map<String, PadUnit.Measure> {
        val map = HashMap<String, PadUnit.Measure>()
        fun put(key: String, m: PadUnit.Measure) {
            map.putIfAbsent(key.lowercase(), m)
        }
        for (cat in UnitConverter.categories) {
            for (u in cat.units) {
                val m = PadUnit.Measure(cat, u)
                put(u.symbol, m)
                val base = u.name.lowercase().replace(Regex(" \\(.*\\)"), "")
                if (!base.contains('/')) {
                    for (spelling in listOf(base, base.replace("metre", "meter").replace("litre", "liter"))) {
                        put(spelling, m)
                        put(spelling + "s", m)
                    }
                }
                if (cat.name == "Area" && u.symbol.endsWith("²")) put("sq " + u.symbol.dropLast(1), m)
            }
        }
        fun alias(unitSymbol: String, vararg keys: String) {
            val m = map[unitSymbol.lowercase()] ?: return
            keys.forEach { put(it, m) }
        }
        alias("in", "inch", "inches")
        alias("ft", "foot", "feet")
        alias("mi", "miles")
        alias("lb", "lbs", "pound", "pounds")
        alias("oz", "ounce", "ounces")
        alias("g", "gram", "grams")
        alias("kg", "kgs", "kilo", "kilos")
        alias("t", "tonnes")
        alias("km/h", "kph", "kmh", "kmph")
        alias("m/s", "mps")
        alias("ft/s", "fps")
        alias("mph", "miles/h")
        alias("kn", "knot", "knots")
        alias("°c", "c", "celsius", "°celsius")
        alias("°f", "f", "fahrenheit", "°fahrenheit")
        alias("k", "kelvin", "kelvins")
        alias("ml", "mls")
        alias("l", "ltr")
        alias("fl oz", "floz", "fluid ounce", "fluid ounces")
        alias("tsp", "teaspoon", "teaspoons")
        alias("tbsp", "tablespoon", "tablespoons")
        alias("gal", "gallons")
        alias("ha", "hectares")
        return map
    }

    private val CURRENCY_NAMES = mapOf(
        "rand" to "ZAR", "dollar" to "USD", "dollars" to "USD", "bucks" to "USD",
        "euro" to "EUR", "euros" to "EUR",
    )

    private fun currencyFor(word: String, rates: Map<String, Double>?): PadUnit.Currency? {
        CURRENCY_NAMES[word]?.let { return PadUnit.Currency(it) }
        if (word.length != 3 || !word.all { it in 'a'..'z' }) return null
        val code = word.uppercase()
        val known = rates?.containsKey(code) == true || CurrencyNames.of(code) != code
        return if (known) PadUnit.Currency(code) else null
    }

    private fun unitForWord(word: String, rates: Map<String, Double>?): PadUnit? =
        UNIT_ALIASES[word] ?: currencyFor(word, rates)

    /** Resolves a unit or currency starting at raw token [j]; tries a two-word phrase ("fl oz") first. */
    private fun resolveAt(toks: List<Tok>, j: Int, rates: Map<String, Double>?): Resolved? {
        val t = toks.getOrNull(j) ?: return null
        if (t is Tok.Cur) return Resolved(PadUnit.Currency(t.code), 1)
        if (t !is Tok.Word) return null
        val t2 = toks.getOrNull(j + 1)
        if (t2 is Tok.Word) {
            UNIT_ALIASES[t.s + " " + t2.s]?.let { return Resolved(it, 2) }
        }
        return unitForWord(t.s, rates)?.let { Resolved(it, 1) }
    }

    // ---- Cleaner: raw tokens → meaningful items -----------------------------------------------

    private sealed interface Item {
        data class Num(val q: PadValue) : Item
        data class Op(val c: Char) : Item
        data class UnitTag(val unit: PadUnit) : Item
        data class ConvTo(val unit: PadUnit) : Item
        data object LP : Item
        data object RP : Item
        data object Pct : Item
    }

    private val CONV_WORDS = setOf("in", "to", "into", "as")
    private val TIMES_WORDS = setOf("at", "x", "times")

    private fun clean(toks: List<Tok>, ctx: Ctx): List<Item> {
        val out = ArrayList<Item>()
        var gap = true // true when a dropped word sits between the last item and the current token
        var depth = 0
        val rates = ctx.rates

        fun emit(item: Item) {
            out.add(item)
            gap = false
        }

        // longest variable phrase starting at token i, or null
        fun variableAt(i: Int): Pair<PadValue, Int>? {
            if (ctx.vars.isEmpty()) return null
            val parts = ArrayList<String>()
            var k = i
            while (k < toks.size && parts.size < 6) {
                val t = toks[k] as? Tok.Word ?: break
                parts.add(t.s); k++
            }
            for (len in parts.size downTo 1) {
                val v = ctx.vars[parts.subList(0, len).joinToString(" ")]
                if (v != null) return v to len
            }
            return null
        }

        var i = 0
        while (i < toks.size) {
            val tok = toks[i]
            val last = out.lastOrNull()
            when (tok) {
                is Tok.Num -> { emit(Item.Num(PadValue(tok.v))); i++ }
                is Tok.Cur -> {
                    val next = toks.getOrNull(i + 1)
                    if (next is Tok.Num) {
                        emit(Item.Num(PadValue(next.v, PadUnit.Currency(tok.code)))); i += 2
                    } else {
                        gap = true; i++
                    }
                }
                is Tok.Op -> { emit(Item.Op(tok.c)); i++ }
                Tok.LP -> { depth++; emit(Item.LP); i++ }
                Tok.RP -> {
                    if (depth > 0) { depth--; emit(Item.RP) } else gap = true
                    i++
                }
                Tok.Pct -> { emit(Item.Pct); i++ }
                Tok.Eq -> { gap = true; i++ }
                is Tok.Word -> {
                    val w = tok.s
                    val lastHasUnit = last is Item.UnitTag || (last is Item.Num && last.q.unit != null)
                    val bareOperand = last is Item.RP || (last is Item.Num && last.q.unit == null && !last.q.percent)

                    // 1. conversion keywords: "in", "to", "into", "as"
                    if (w in CONV_WORDS) {
                        val next = resolveAt(toks, i + 1, rates)
                        // "12 in", "12 in cm": the number is in inches
                        if (w == "in" && !gap && last is Item.Num && last.q.unit == null && !last.q.percent &&
                            (next == null || (next.unit is PadUnit.Measure && next.unit.category.name == "Length"))
                        ) {
                            emit(Item.UnitTag(UNIT_ALIASES.getValue("in"))); i++
                            continue
                        }
                        val operandBefore = last is Item.Num || last is Item.RP || last is Item.UnitTag
                        if (operandBefore && next != null) {
                            emit(Item.ConvTo(next.unit)); i += 1 + next.words
                            continue
                        }
                        gap = true; i++
                        continue
                    }

                    // 2. a unit right after a number: "5 km", "100 USD"; after a unit it means "convert to"
                    if (!gap && (bareOperand || lastHasUnit)) {
                        val r = resolveAt(toks, i, rates)
                        if (r != null) {
                            emit(if (bareOperand) Item.UnitTag(r.unit) else Item.ConvTo(r.unit))
                            i += r.words
                            continue
                        }
                    }

                    // 3. variables
                    val v = variableAt(i)
                    if (v != null) {
                        emit(Item.Num(v.first)); i += v.second
                        continue
                    }

                    // 4. keywords that stand for a value
                    when (w) {
                        "total", "sum", "subtotal" -> {
                            val t = ctx.total()
                            if (t != null) {
                                ctx.usedTotal = true
                                emit(Item.Num(t)); i++
                                continue
                            }
                        }
                        "ans", "prev", "previous" -> {
                            val p = ctx.previous
                            if (p != null) {
                                emit(Item.Num(p)); i++
                                continue
                            }
                        }
                    }

                    // 5. "of" after a percentage and "at"/"x"/"times" between values mean multiply
                    val percentBefore = last is Item.Pct || (last is Item.Num && last.q.percent)
                    val operandish = last is Item.Num || last is Item.RP || last is Item.UnitTag || last is Item.Pct
                    if (w == "of" && percentBefore) { emit(Item.Op('*')); i++; continue }
                    if (w in TIMES_WORDS && operandish) { emit(Item.Op('*')); i++; continue }
                    // "15 percent"
                    if (w == "percent" && !gap && last is Item.Num) { emit(Item.Pct); i++; continue }

                    // 6. anything else is a label
                    gap = true
                    i++
                }
            }
        }

        // Half-typed endings: drop dangling operators and open parentheses; close the rest.
        while (out.isNotEmpty()) {
            val l = out.last()
            if (l is Item.Op || l == Item.LP) out.removeAt(out.size - 1) else break
        }
        var open = 0
        for (item in out) {
            if (item == Item.LP) open++ else if (item == Item.RP) open--
        }
        repeat(open.coerceAtLeast(0)) { out.add(Item.RP) }
        return out
    }

    // ---- Parser -------------------------------------------------------------------------------

    private object Bail : RuntimeException() {
        override fun fillInStackTrace(): Throwable = this
    }

    private class Parser(private val items: List<Item>, private val rates: Map<String, Double>?) {
        private var p = 0
        private var depth = 0

        fun parseAll(): PadValue? {
            if (items.isEmpty()) return null
            return try {
                val v = expr()
                if (p != items.size) null else v
            } catch (e: Bail) {
                null
            }
        }

        private fun peek(): Item? = items.getOrNull(p)

        private fun enter() {
            if (++depth > MAX_DEPTH) throw Bail
        }

        private fun expr(): PadValue {
            var left = term()
            while (true) {
                val t = peek()
                if (t is Item.Op && (t.c == '+' || t.c == '-')) {
                    p++
                    left = addSub(left, term(), t.c == '+')
                } else if (t is Item.ConvTo) {
                    p++
                    left = convert(left, t.unit)
                } else {
                    return left
                }
            }
        }

        private fun startsPrimary(t: Item?) = t is Item.Num || t === Item.LP

        private fun term(): PadValue {
            var left = power()
            while (true) {
                val t = peek()
                if (t is Item.Op && (t.c == '*' || t.c == '/')) {
                    p++
                    left = mulDiv(left, unary(), t.c)
                } else if (startsPrimary(t)) {
                    left = mulDiv(left, power(), '*') // "3 45", "2(3+4)"
                } else {
                    return left
                }
            }
        }

        private fun unary(): PadValue {
            val t = peek()
            if (t is Item.Op && (t.c == '-' || t.c == '+')) {
                p++
                enter()
                val v = unary()
                depth--
                return if (t.c == '-') v.copy(number = -v.number) else v
            }
            return power()
        }

        private fun power(): PadValue {
            val base = postfix()
            val t = peek()
            if (t is Item.Op && t.c == '^') {
                p++
                enter()
                val exp = unary()
                depth--
                if (base.unit != null || exp.unit != null) throw Bail
                val r = base.number.pow(exp.number)
                if (!r.isFinite()) throw Bail
                return PadValue(r)
            }
            return base
        }

        private fun postfix(): PadValue {
            var n = primary()
            while (true) {
                when (val t = peek()) {
                    Item.Pct -> {
                        p++
                        if (n.unit != null || n.percent) throw Bail
                        n = PadValue(n.number / 100.0, null, percent = true)
                    }
                    is Item.UnitTag -> {
                        p++
                        if (n.percent) throw Bail
                        n = if (n.unit == null) n.copy(unit = t.unit) else convert(n, t.unit)
                    }
                    else -> return n
                }
            }
        }

        private fun primary(): PadValue {
            val t = peek()
            if (t is Item.Num) { p++; return t.q }
            if (t === Item.LP) {
                p++
                enter()
                val v = expr()
                depth--
                if (peek() === Item.RP) p++
                return v
            }
            throw Bail
        }

        // ---- arithmetic with units and percentages ----

        fun addSub(l: PadValue, r: PadValue, plus: Boolean): PadValue {
            val sign = if (plus) 1.0 else -1.0
            // 100 + 15% → 115 (the percentage is of the left side)
            if (r.percent && !l.percent) return PadValue(l.number + sign * l.number * r.number, l.unit)
            if (l.percent && r.percent) return PadValue(l.number + sign * r.number, null, percent = true)
            if (l.percent) return PadValue(l.number + sign * r.number, r.unit)
            val lu = l.unit
            val ru = r.unit
            return when {
                lu == null -> PadValue(l.number + sign * r.number, ru)
                ru == null -> PadValue(l.number + sign * r.number, lu)
                else -> PadValue(l.number + sign * (convertNumber(r.number, ru, lu) ?: throw Bail), lu)
            }
        }

        private fun mulDiv(l: PadValue, r: PadValue, op: Char): PadValue {
            if (op == '*') {
                if (l.unit != null && r.unit != null) throw Bail
                return PadValue(l.number * r.number, l.unit ?: r.unit)
            }
            if (r.number == 0.0) throw Bail
            val lu = l.unit
            val ru = r.unit
            return when {
                lu != null && ru != null -> {
                    val denominator = convertNumber(r.number, ru, lu) ?: throw Bail
                    if (denominator == 0.0) throw Bail
                    PadValue(l.number / denominator)
                }
                ru != null -> throw Bail
                else -> PadValue(l.number / r.number, lu)
            }
        }

        private fun convert(q: PadValue, target: PadUnit): PadValue {
            if (q.percent) throw Bail
            val from = q.unit ?: return q.copy(unit = target) // "100 in ZAR" labels a bare number
            return PadValue(convertNumber(q.number, from, target) ?: throw Bail, target)
        }

        private fun convertNumber(v: Double, from: PadUnit, to: PadUnit): Double? {
            if (from == to) return v
            if (from is PadUnit.Currency && to is PadUnit.Currency) {
                return CurrencyConverter.convert(v, from.code, to.code, rates ?: return null)
            }
            if (from is PadUnit.Measure && to is PadUnit.Measure && from.category === to.category) {
                return UnitConverter.convert(v, from.unit, to.unit)
            }
            return null
        }
    }
}
