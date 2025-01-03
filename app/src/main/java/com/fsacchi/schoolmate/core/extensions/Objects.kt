package com.fsacchi.schoolmate.core.extensions

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

object DateMasks {
    const val sortFormat = "dd/MM/yy"
    const val appFormat = "dd/MM/yyyy"
    const val hourFormat = "HH:mm:ss"
    const val listFormat = "d MMMM yyyy"
    const val extensiveFormat = "EEEE, dd MMMM yyyy"
    const val serverFormat = "yyyy-MM-dd"
    const val extensiveFormatDayWeek = "EEEE, d 'de' MMMM 'de' yyyy"
    const val todayFormat = "'Hoje, 'd 'de' MMMM 'de' yyyy"
}

internal val locale = Locale("pt", "BR")

//region Double
fun Double.safePositive(): Double = if (this < 0.0) 0.0 else this
fun Double.abs(): Double = kotlin.math.abs(this)

fun Double.currencyFormat(useSymbol: Boolean = true): String {
    val numberFormat = NumberFormat.getCurrencyInstance(locale) as DecimalFormat
    val symbols = numberFormat.decimalFormatSymbols?.apply {
        currencySymbol = if (useSymbol) "$currencySymbol " else ""
    }

    numberFormat.decimalFormatSymbols = symbols
    return numberFormat.format(this).trim()
}

fun Double.valueByPercentage(percentage: Double): Double {
    return (percentage * this) / 100
}

fun Double.stringToValue(): String {
    return this.currencyFormat(false).replace(",", ".")
}

fun Double.formatTwoDecimalPlaces(): String {
    return String.format("%.2f", this).replace(",", ".")
}

//endregionappFormat

//region String
fun String.bigDecimal(): BigDecimal {
    return try {
        val clean = replace("\\D".toRegex(), "")
        BigDecimal(clean).movePointLeft(2)
    } catch (e: Exception) {
        0.toBigDecimal()
    }
}

fun String.toLower(): String {
    return lowercase(locale)
}

fun String.toUpper(): String {
    return uppercase(locale)
}

fun String.maskString(mask: String): String {
    val result = StringBuilder()
    var i = 0
    var j = 0
    while (result.length != mask.length) {
        result.append(
            if (mask[i] == '#' || mask[i] == '@' || mask[i] == '%') this[j++] else mask[i]
        )
        i++
    }
    return result.toString()
}

fun String.unmask(): String {
    return this.replace("[ /.()-]".toRegex(), "")
}

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

fun String.isDigit() = this.all { it.isDigit() }

fun String.isValidName(allowNumber: Boolean = false): Boolean {

    if (this.contains("_") || this.contains("\\") || this.contains("×") || this.contains("÷") ||
        this.contains("[") || this.contains("]") || this.contains("^")
    ) {
        return false
    }

    val expression = if (!allowNumber) "^[aA-zZÀ-ü\\-\\'\\_\\s]+\$" else "^[aA-zZÀ-ü0-9\\-\\'\\_\\s]+\$"
    val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(this)
    return matcher.matches()
}

fun String.isEmailValid(): Boolean {
    val expression = "^[\\w.+_-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
    val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(this)
    return matcher.matches()
}

fun String.isValidPhone() = Pattern.matches("[1-9]{2} [0-9]{4}-[0-9]{4}", this)
fun String.isValidCellphone() = Pattern.matches("[1-9]{2} [9][0-9]{4}-[0-9]{4}", this)

fun String.isValidZipCode() = Pattern.matches("[0-9]{5}-[0-9]{3}", this)

fun String.formatAccount() = """${slice(0..length - 2)}-${last()}"""

fun String.capitalize() = capitalize(locale)

fun String.capitalizeFirstLetter(): String {
    return if (this.isNotEmpty()) {
        this.substring(0, 1).uppercase() + this.substring(1)
    } else {
        this
    }
}

fun String.execIfEmpty(exec: () -> Unit) {
    if (this.isBlank()) exec()
}

//endregion

//region List
fun <E> List<E>.toArrayList() = ArrayList(this)
fun <E> List<E>.hasJustOne() = size == 1
//endregion

//region UUID
fun Any.randomUUID() = UUID.randomUUID().toString()
//endregion

//region enum
inline fun <reified T : Enum<T>> T.name(): String = name
inline fun <reified T : Enum<T>> String.toEnum(): T = enumValueOf(this)
//endregion

fun emptyString(): String = ""

fun Exception.handleFirebaseErrors(): String = run {
    val errorMessage = when (this) {
        is FirebaseAuthInvalidCredentialsException -> "Credenciais inválidas"
        is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado. Tente outro e-mail."
        is FirebaseAuthEmailException -> "Erro ao enviar o e-mail. Tente novamente mais tarde."
        is FirebaseNetworkException -> "Erro de conexão, verifique sua internet."
        is FirebaseFirestoreException -> "Erro ao gravar informação"
        else -> "Erro desconhecido. Tente novamente."
    }
    return errorMessage
}

fun Int.isEmoji(): Boolean {
    val codePoint = this
    return (codePoint in 0x1F600..0x1F64F ||
            codePoint in 0x1F300..0x1F5FF ||
            codePoint in 0x1F680..0x1F6FF ||
            codePoint in 0x1F700..0x1F77F ||
            codePoint in 0x2600..0x26FF ||
            codePoint in 0x2700..0x27BF ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint in 0x1F900..0x1F9FF ||
            codePoint in 0x1FA70..0x1FAFF)
}

fun Int.toEmoji(): String = String(Character.toChars(this))
fun String.fromInt(): Int? = this.codePoints().toArray().firstOrNull()
