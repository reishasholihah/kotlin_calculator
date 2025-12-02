import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class CalculatorViewModel {
    private var currentInput = "0"
    private var currentExpression = ""
    private var operator: String? = null
    private var firstOperand: BigDecimal? = null
    private var waitingForOperand = false
    private var xorFirst: Long? = null
    private var isNewEntry = false

    val isDisplayingError: Boolean
        get() = currentInput.startsWith("Error:")

    val displayText: String
        get() = if (currentExpression.isNotEmpty() && !isDisplayingError) {
            currentExpression
        } else {
            currentInput
        }

    private fun setError(message: String) {
        currentInput = "Error: $message"
        currentExpression = ""
        waitingForOperand = true
    }

    private fun clearErrorIfNeeded() {
        if (isDisplayingError) {
            currentInput = ""
            currentExpression = ""
        }
    }

    fun inputDigit(digit: Char) {
        clearErrorIfNeeded()

        if (isNewEntry) {
            currentInput = digit.toString()
            currentExpression = ""
            isNewEntry = false
            waitingForOperand = false
            operator = null
            xorFirst = null
            return
        }

        if (waitingForOperand) {
            currentInput = digit.toString()
            waitingForOperand = false
            if (operator != null && operator != "XOR") {
                currentExpression = ""
            }
        } else {
            currentInput = if (currentInput == "0") {
                digit.toString()
            } else {
                currentInput + digit
            }
        }

        if (operator != null && !isSpecialMode()) {
            if (operator == "XOR") {
                currentExpression = "$xorFirst XOR $currentInput"
            } else {
                val first = formatForDisplay(firstOperand)
                val secondBd = parseBigDecimal(currentInput)
                val second = formatForDisplay(secondBd)
                currentExpression = "$first $operator $second"
            }
        }
    }

    fun inputDecimal() {
        clearErrorIfNeeded()

        if (isNewEntry) {
            currentInput = "0."
            currentExpression = ""
            isNewEntry = false
            waitingForOperand = false
            operator = null
            xorFirst = null
            return
        }

        if (operator == "XOR") return

        if (waitingForOperand) {
            currentInput = "0."
            waitingForOperand = false
            if (operator != null) {
                currentExpression = ""
            }
        } else if (!currentInput.contains(".")) {
            currentInput = if (currentInput.isEmpty() || currentInput == "0") {
                "0."
            } else {
                "$currentInput."
            }
        }

        if (operator != null && operator != "XOR" && !isSpecialMode()) {
            val first = formatForDisplay(firstOperand!!)
            val second = currentInput
            currentExpression = "$first $operator $second"
        }
    }

    fun clear() {
        currentInput = "0"
        currentExpression = ""
        operator = null
        firstOperand = null
        waitingForOperand = false
        xorFirst = null
    }

    fun setOperator(op: String) {
        if (isDisplayingError) return

        if (isNewEntry) {
            val num = parseBigDecimal(currentInput)
            if (num == null) {
                setError("Invalid number")
                return
            }
            firstOperand = num
            currentExpression = ""
            isNewEntry = false
        } else {
            val currentNum = parseBigDecimal(currentInput)
            if (currentNum == null) {
                setError("Invalid number")
                return
            }

            if (operator == null) {
                firstOperand = currentNum
            } else if (!waitingForOperand) {
                calculateSilently()
                if (isDisplayingError) return
            }
        }

        operator = op
        val firstClean = formatForDisplay(firstOperand!!)
        currentExpression = "$firstClean $op"
        waitingForOperand = true
    }

    fun calculate() {
        if (operator == null || isDisplayingError) return

        val secondText = currentInput
        val secondOperand = parseBigDecimal(secondText)
        if (secondOperand == null) {
            setError("Invalid number")
            return
        }

        if (firstOperand == null) {
            setError("Missing first operand")
            return
        }

        var result: BigDecimal?
        try {
            result = when (operator) {
                "+" -> firstOperand!! + secondOperand
                "-" -> firstOperand!! - secondOperand
                "*" -> firstOperand!! * secondOperand
                "/" -> {
                    if (secondOperand == BigDecimal.ZERO) {
                        setError("Division by zero")
                        return
                    }
                    firstOperand!!.divide(secondOperand, 15, RoundingMode.HALF_UP)
                }
                "%" -> {
                    if (secondOperand == BigDecimal.ZERO) {
                        setError("Modulo by zero")
                        return
                    }
                    firstOperand!!.remainder(secondOperand)
                }
                else -> {
                    setError("Unknown operator")
                    return
                }
            }
        } catch (_: Exception) {
            setError("Calculation error")
            return
        }

        val resultStr = formatForDisplay(result!!)
        val firstClean = formatForDisplay(firstOperand!!)
        val secondClean = formatForDisplay(secondOperand)

        currentInput = resultStr
        currentExpression = "$firstClean ${operator!!} $secondClean = $resultStr"
        operator = null
        waitingForOperand = true
        isNewEntry = true
    }

    private fun calculateSilently() {
        val secondOperand = parseBigDecimal(currentInput) ?: BigDecimal.ZERO
        if (firstOperand == null) return

        var result: BigDecimal?
        try {
            result = when (operator) {
                "+" -> firstOperand!! + secondOperand
                "-" -> firstOperand!! - secondOperand
                "*" -> firstOperand!! * secondOperand
                "/" -> {
                    if (secondOperand == BigDecimal.ZERO) {
                        setError("Division by zero")
                        return
                    }
                    firstOperand!!.divide(secondOperand, 15, RoundingMode.HALF_UP)
                }
                "%" -> firstOperand!!.remainder(secondOperand)
                else -> return
            }
        } catch (_: Exception) {
            setError("Calculation failed")
            return
        }

        currentInput = formatForDisplay(result!!)
        firstOperand = parseBigDecimal(currentInput)
        currentExpression = ""
    }

    fun applyTrigFunction(func: String) {
        if (isDisplayingError) return
        clearErrorIfNeeded()

        val input = currentInput.toDoubleOrNull()
        if (input == null) {
            setError("Enter a number for trig")
            return
        }

        val radians = Math.toRadians(input)
        val result = when (func) {
            "sin" -> sin(radians)
            "cos" -> cos(radians)
            "tan" -> {
                val deg = input % 180.0
                if (abs(abs(deg) - 90.0) < 1e-10) {
                    setError("tan(90°) is undefined")
                    return
                }
                tan(radians)
            }
            else -> {
                setError("Unknown trig function")
                return
            }
        }

        currentExpression = "$func($input) = ${formatDouble(result)}"
        currentInput = formatDouble(result)
        waitingForOperand = true
        isNewEntry = true
    }

    fun startXor() {
        if (isDisplayingError) return
        clearErrorIfNeeded()

        val valueText = if (isNewEntry) currentInput else currentInput
        val value = valueText.toLongOrNull()
        if (value == null || value < 0) {
            setError("XOR needs non-negative integer")
            return
        }

        xorFirst = value
        operator = "XOR"
        currentExpression = "$value XOR"
        waitingForOperand = true
        isNewEntry = false
    }

    fun completeXor() {
        if (operator != "XOR" || xorFirst == null) return

        val second = currentInput.toLongOrNull()
        if (second == null || second < 0) {
            setError("XOR needs non-negative integer")
            resetXorState()
            return
        }

        val result = xorFirst!! xor second
        currentExpression = "$xorFirst XOR $second = $result"
        currentInput = result.toString()
        resetXorState()
        isNewEntry = true
    }

    private fun resetXorState() {
        operator = null
        xorFirst = null
        waitingForOperand = true
    }

    fun convertToBase(base: Int) {
        if (isDisplayingError) return
        clearErrorIfNeeded()

        val value = currentInput.toLongOrNull()
        if (value == null || value < 0) {
            setError("Base conversion needs non-negative integer")
            return
        }

        val converted = when (base) {
            2 -> value.toString(2)
            16 -> value.toString(16).uppercase()
            else -> {
                setError("Unsupported base")
                return
            }
        }

        val baseName = if (base == 2) "BIN" else "HEX"
        currentExpression = "$baseName($value) = $converted"
        currentInput = converted
        waitingForOperand = true
        isNewEntry = true
    }

    fun onEqualsPressed() {
        if (operator == "XOR") {
            completeXor()
        } else if (operator != null) {
            calculate()
        }
    }

    private fun isSpecialMode(): Boolean {
        return currentExpression.contains("(")
    }

    private fun formatDouble(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) {
            "Error"
        } else if (value % 1 == 0.0) {
            value.toLong().toString()
        } else {
            "%.10g".format(value).replace("E", "e")
        }
    }

    private fun formatForDisplay(value: BigDecimal?): String {
        if (value == null) return "0"
        var str = value.stripTrailingZeros().toPlainString()
        if (str == "-0" || str == "-0.0") str = "0"
        return str
    }

    private fun parseBigDecimal(text: String): BigDecimal? {
        return try {
            if (text.contains("E", ignoreCase = true)) {
                BigDecimal(text.toDouble().toString())
            } else {
                BigDecimal(text)
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}