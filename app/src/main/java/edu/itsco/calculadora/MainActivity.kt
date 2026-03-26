package edu.itsco.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.itsco.calculadora.ui.theme.CalculadoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalculadoraScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

object CalcColors {
    val NumFondo     = Color(0xFF1E2050)
    val NumTexto     = Color(0xFFCDD4FF)
    val EspFondo     = Color(0xFF0D3D2E)
    val EspTexto     = Color(0xFF4DFFC3)
    val OpNaranja    = Color(0xFFFF6B35)
    val OpRosa       = Color(0xFFFF2D78)
    val OpTexto      = Color.White
    val IgualA       = Color(0xFF00C6FF)
    val IgualB       = Color(0xFF0066FF)
    val IgualTexto   = Color.White
    val NeutroFondo  = Color(0xFF252545)
    val NeutroTexto  = Color(0xFFAAB4FF)
    val DisplayTexto = Color(0xFFEEF0FF)
    val DisplaySub   = Color(0xFF6B7FD4)
}

enum class TipoBoton { NUMERO, ESPECIAL, OPERADOR, IGUAL, NEUTRO }
class Evaluador(private val expr: String) {
    private var pos = 0
    fun evaluar(): Double = parseExpresion()
    private fun parseExpresion(): Double {
        var r = parseTerm()
        while (pos < expr.length && (expr[pos] == '+' || expr[pos] == '-')) {
            val op = expr[pos++]
            val d = parseTerm()
            r = if (op == '+') r + d else r - d
        }
        return r
    }

    private fun parseTerm(): Double {
        var r = parseFactor()
        while (pos < expr.length && (expr[pos] == '*' || expr[pos] == '/')) {
            val op = expr[pos++]
            val d = parseFactor()
            r = if (op == '*') r * d else r / d
        }
        return r
    }

    private fun parseFactor(): Double {
        skip()
        if (pos < expr.length && expr[pos] == '-') { pos++; return -parseFactor() }
        if (pos < expr.length && expr[pos] == '(') {
            pos++
            val r = parseExpresion()
            skip()
            if (pos < expr.length && expr[pos] == ')') pos++
            return r
        }
        return parseNum()
    }

    private fun parseNum(): Double {
        skip()
        val ini = pos
        if (pos < expr.length && expr[pos] == '-') pos++
        while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) pos++
        return expr.substring(ini, pos).toDoubleOrNull() ?: 0.0
    }
    private fun skip() { while (pos < expr.length && expr[pos] == ' ') pos++ }
}

fun calcular(expr: String): String {
    return try {
        if (expr.isBlank()) return "0"
        val limpia = expr
            .replace("×", "*").replace("÷", "/").replace("−", "-")
            .let { e -> e + ")".repeat(maxOf(0, e.count { it == '(' } - e.count { it == ')' })) }
        val r = Evaluador(limpia).evaluar()
        when {
            r.isNaN() || r.isInfinite() -> "Error"
            r % 1.0 == 0.0 -> r.toLong().toString()
            else -> r.toBigDecimal().stripTrailingZeros().toPlainString()
        }
    } catch (e: Exception) { "Error" }
}

fun esOp(c: Char?): Boolean = c in listOf('+', '-', '×', '÷')

@Composable
fun CalculadoraScreen(modifier: Modifier = Modifier) {
    var expresion        by remember { mutableStateOf("") }
    var display          by remember { mutableStateOf("0") }
    var parentesisAbiertos by remember { mutableIntStateOf(0) }
    var recienIgual      by remember { mutableStateOf(false) }
    var cCount           by remember { mutableIntStateOf(0) }
    fun formatear(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        return if (v % 1.0 == 0.0) v.toLong().toString()
        else v.toBigDecimal().stripTrailingZeros().toPlainString()
    }

    fun ultimoChar(): Char? = expresion.trimEnd().lastOrNull()
    fun ingresarDigito(d: String) {
        cCount = 0
        if (recienIgual) {
            expresion = d
            recienIgual = false
        } else {
            val uc = ultimoChar()
            if (uc == ')') {
                expresion += "×$d"
            } else {
                expresion = if (expresion == "0" || expresion.isEmpty()) d else expresion + d
            }
        }
        display = expresion
    }

    fun ingresarPunto() {
        cCount = 0
        if (recienIgual) { expresion = "0."; recienIgual = false; display = expresion; return }
        val uc = ultimoChar()
        if (uc == null || esOp(uc) || uc == '(') {
            expresion += "0."
        } else if (uc?.isDigit() == true) {
            val ultimoSegmento = expresion.split("+", "-", "×", "÷", "(", ")").last()
            if (!ultimoSegmento.contains(".")) expresion += "."
        }
        display = expresion
    }

    fun ingresarOperador(op: String) {
        cCount = 0
        if (recienIgual) recienIgual = false
        val uc = ultimoChar()
        when {
            expresion.isEmpty() || expresion == "0" -> {
                if (op == "-") { expresion = "-"; display = expresion; return }
            }
            esOp(uc) -> {
                expresion = expresion.dropLast(1) + op
                display = expresion
                return
            }
            uc == '(' -> {
                if (op == "-") { expresion += op; display = expresion; return }
                return
            }
        }
        expresion += op
        display = expresion
    }

    fun alternarSigno() {
        cCount = 0
        if (expresion.isEmpty() || expresion == "0") return
        val uc = ultimoChar()
        if (uc?.isDigit() == true || uc == ')') {
            val ultimoOp = expresion.indexOfLast { esOp(it) || it == '(' }
            val numPart = expresion.substring(ultimoOp + 1)
            val prefix = expresion.substring(0, ultimoOp + 1)
            expresion = if (numPart.startsWith("-")) prefix + numPart.removePrefix("-")
            else "$prefix-$numPart"
            display = expresion
        }
    }

    fun porcentaje() {
        cCount = 0
        val uc = ultimoChar()
        if (uc?.isDigit() == true || uc == ')') {
            val ultimoOp = expresion.indexOfLast { esOp(it) || it == '(' }
            val numPart = expresion.substring(ultimoOp + 1)
            val prefix = expresion.substring(0, ultimoOp + 1)
            val v = try { calcular(numPart).toDouble() } catch (e: Exception) { return }
            expresion = prefix + formatear(v / 100.0)
            display = expresion
        }
    }

    fun ingresarParentesis() {
        cCount = 0
        if (recienIgual) { expresion = "("; parentesisAbiertos = 1; recienIgual = false; display = expresion; return }
        val uc = ultimoChar()
        val debeAbrir = when {
            expresion.isEmpty() || expresion == "0" -> true
            uc == '(' -> true
            esOp(uc) -> true
            parentesisAbiertos == 0 -> {
                if (uc?.isDigit() == true || uc == ')') {
                    expresion += "×"
                }
                true
            }
            uc?.isDigit() == true || uc == '.' -> false
            uc == ')' -> false
            else -> true
        }
        if (debeAbrir) {
            expresion += "("
            parentesisAbiertos++
        } else {
            expresion += ")"
            parentesisAbiertos--
        }
        display = expresion
    }

    fun manejarIgual() {
        cCount = 0
        if (expresion.isEmpty() || expresion == "0") return
        val exprCompleta = expresion + ")".repeat(parentesisAbiertos)
        val resultado = calcular(exprCompleta)
        expresion = resultado
        display = resultado
        parentesisAbiertos = 0
        recienIgual = true
    }

    fun manejarC() {
        cCount++
        if (cCount >= 2) {
            expresion = ""
            display = "0"
            parentesisAbiertos = 0
            recienIgual = false
            cCount = 0
        } else {
            if (expresion.isNotEmpty()) {
                val removido = expresion.last()
                if (removido == '(') parentesisAbiertos = maxOf(0, parentesisAbiertos - 1)
                else if (removido == ')') parentesisAbiertos++
                expresion = expresion.dropLast(1)
            }
            display = if (expresion.isEmpty()) "0" else expresion
            recienIgual = false
        }
    }

    val textoArriba = if (parentesisAbiertos > 0) "( ".repeat(parentesisAbiertos).trimEnd() else ""
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D14), Color(0xFF0A0A1E))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF16162A), Color(0xFF1A1A35))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = textoArriba,
                        color = CalcColors.DisplaySub,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 26.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = display,
                        color = CalcColors.DisplayTexto,
                        fontSize = when {
                            display.length > 12 -> 28.sp
                            display.length > 8  -> 38.sp
                            else                -> 54.sp
                        },
                        fontWeight = FontWeight.ExtraLight,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            FilaBotones {
                BotonCalc("C",  TipoBoton.ESPECIAL, Modifier.weight(1f)) { manejarC() }
                BotonCalc("()", TipoBoton.ESPECIAL, Modifier.weight(1f)) { ingresarParentesis() }
                BotonCalc("%",  TipoBoton.ESPECIAL, Modifier.weight(1f)) { porcentaje() }
                BotonCalc("÷",  TipoBoton.OPERADOR, Modifier.weight(1f)) { ingresarOperador("÷") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            FilaBotones {
                BotonCalc("7", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("7") }
                BotonCalc("8", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("8") }
                BotonCalc("9", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("9") }
                BotonCalc("×", TipoBoton.OPERADOR, Modifier.weight(1f)) { ingresarOperador("×") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            FilaBotones {
                BotonCalc("4", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("4") }
                BotonCalc("5", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("5") }
                BotonCalc("6", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("6") }
                BotonCalc("-", TipoBoton.OPERADOR, Modifier.weight(1f)) { ingresarOperador("-") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            FilaBotones {
                BotonCalc("1", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("1") }
                BotonCalc("2", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("2") }
                BotonCalc("3", TipoBoton.NUMERO,   Modifier.weight(1f)) { ingresarDigito("3") }
                BotonCalc("+", TipoBoton.OPERADOR, Modifier.weight(1f)) { ingresarOperador("+") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            FilaBotones {
                BotonCalc("+/-", TipoBoton.NEUTRO, Modifier.weight(1f)) { alternarSigno() }
                BotonCalc("0",   TipoBoton.NUMERO, Modifier.weight(1f)) { ingresarDigito("0") }
                BotonCalc(".",   TipoBoton.NEUTRO, Modifier.weight(1f)) { ingresarPunto() }
                BotonCalc("=",   TipoBoton.IGUAL,  Modifier.weight(1f)) { manejarIgual() }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun FilaBotones(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}
@Composable
fun BotonCalc(
    texto: String,
    tipo: TipoBoton,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var presionado by remember { mutableStateOf(false) }
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.91f else 1f,
        animationSpec = tween(80),
        label = "escala"
    )
    val (fondoInicio, fondoFin, textoColor) = when (tipo) {
        TipoBoton.NUMERO   -> Triple(CalcColors.NumFondo,    CalcColors.NumFondo.copy(alpha = 0.7f), CalcColors.NumTexto)
        TipoBoton.ESPECIAL -> Triple(CalcColors.EspFondo,    CalcColors.EspFondo.copy(alpha = 0.6f), CalcColors.EspTexto)
        TipoBoton.OPERADOR -> Triple(CalcColors.OpNaranja,   CalcColors.OpRosa,                      CalcColors.OpTexto)
        TipoBoton.IGUAL    -> Triple(CalcColors.IgualA,      CalcColors.IgualB,                      CalcColors.IgualTexto)
        TipoBoton.NEUTRO   -> Triple(CalcColors.NeutroFondo, CalcColors.NeutroFondo,                 CalcColors.NeutroTexto)
    }
    Box(
        modifier = modifier
            .height(72.dp)
            .scale(escala)
            .background(
                Brush.linearGradient(listOf(fondoInicio, fondoFin)),
                RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { presionado = true; onClick(); presionado = false },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor   = textoColor
            ),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text       = texto,
                fontSize   = if (texto.length > 2) 17.sp else 24.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textoColor
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalculadoraPreview() {
    CalculadoraTheme {
        CalculadoraScreen()
    }
}