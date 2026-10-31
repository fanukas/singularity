package dev.fanukas.singularity.engine

/**
 * A small JSON reader and writer, so the engine has no dependencies and runs the same on the phone,
 * the server and in tests. Objects become LinkedHashMap<String, Any?>, arrays List<Any?>,
 * numbers Double, plus String, Boolean and null.
 */
object Json {
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.ws()
        val v = p.value()
        p.ws()
        if (p.i != text.length) p.fail("trailing characters")
        return v
    }

    fun write(value: Any?): String = StringBuilder().also { writeTo(it, value) }.toString()

    private fun writeTo(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> writeString(sb, v)
            is Boolean -> sb.append(v)
            is Int, is Long -> sb.append(v)
            is Number -> {
                val d = v.toDouble()
                require(d.isFinite()) { "JSON can't hold $d" }
                if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append(d.toLong()) else sb.append(d)
            }
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, k.toString()); sb.append(':'); writeTo(sb, x)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (x in v) { if (!first) sb.append(','); first = false; writeTo(sb, x) }
                sb.append(']')
            }
            is IntArray -> writeTo(sb, v.toList())
            is DoubleArray -> writeTo(sb, v.toList())
            is BooleanArray -> writeTo(sb, v.toList())
            else -> writeString(sb, v.toString())
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
        }
        sb.append('"')
    }

    private class Parser(val s: String) {
        var i = 0
        fun fail(msg: String): Nothing = throw IllegalArgumentException("JSON: $msg at $i")
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun value(): Any? {
            if (i >= s.length) fail("unexpected end")
            return when (s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> num()
            }
        }
        fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) fail("bad literal")
            i += word.length
            return v
        }
        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++; ws()
            if (s[i] == '}') { i++; return m }
            while (true) {
                ws(); if (s[i] != '"') fail("expected key")
                val k = str(); ws()
                if (s[i] != ':') fail("expected :")
                i++; ws()
                m[k] = value(); ws()
                when (s[i]) { ',' -> i++; '}' -> { i++; return m }; else -> fail("expected , or }") }
            }
        }
        fun arr(): List<Any?> {
            val l = ArrayList<Any?>()
            i++; ws()
            if (s[i] == ']') { i++; return l }
            while (true) {
                ws(); l.add(value()); ws()
                when (s[i]) { ',' -> i++; ']' -> { i++; return l }; else -> fail("expected , or ]") }
            }
        }
        fun str(): String {
            val sb = StringBuilder()
            i++
            while (true) {
                if (i >= s.length) fail("unterminated string")
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = s[i++]
                        when (e) {
                            'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                            'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }
        fun num(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            if (start == i) fail("unexpected character '${s[i]}'")
            return s.substring(start, i).toDouble()
        }
    }
}

// Small helpers for reading parsed JSON.
@Suppress("UNCHECKED_CAST")
internal fun Any?.obj(): Map<String, Any?> = this as? Map<String, Any?> ?: error("expected object")
internal fun Any?.list(): List<Any?> = this as? List<Any?> ?: error("expected array")
internal fun Map<String, Any?>.str(k: String): String = this[k] as? String ?: error("missing text '$k'")
internal fun Map<String, Any?>.strOr(k: String, d: String): String = this[k] as? String ?: d
internal fun Map<String, Any?>.num(k: String): Double = (this[k] as? Number)?.toDouble() ?: error("missing number '$k'")
internal fun Map<String, Any?>.int(k: String): Int = num(k).toInt()
internal fun Map<String, Any?>.bool(k: String, d: Boolean = false): Boolean = this[k] as? Boolean ?: d
