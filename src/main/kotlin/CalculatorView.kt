import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane

class CalculatorView(private val viewModel: CalculatorViewModel) {

    private val displayLabel = Label("0").apply {
        styleClass.add("display")
        maxWidth = Double.MAX_VALUE
        isFocusTraversable = false
    }

    private val display = ScrollPane(displayLabel).apply {
        hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        isFitToHeight = true
        padding = javafx.geometry.Insets(0.0)
    }

    fun createRoot(): BorderPane {
        val root = BorderPane().apply {
            top = display
            center = createButtonGrid()
            stylesheets.add(javaClass.getResource("/style.css")?.toExternalForm() ?: "")
        }
        updateDisplay()
        return root
    }

    private fun createButtonGrid(): VBox {
        val grid = GridPane().apply {
            alignment = Pos.CENTER
            hgap = 8.0
            vgap = 8.0

            val buttons = arrayOf(
                button("sin", "special"),
                button("cos", "special"),
                button("tan", "special"),
                button("C", "clear"),

                button("BIN", "special"),
                button("HEX", "special"),
                button("XOR", "special"),
                button("/", "operator"),

                button("7", "number"),
                button("8", "number"),
                button("9", "number"),
                button("*", "operator"),

                button("4", "number"),
                button("5", "number"),
                button("6", "number"),
                button("-", "operator"),

                button("1", "number"),
                button("2", "number"),
                button("3", "number"),
                button("+", "operator"),

                button(".", "number"),
                button("0", "number"),
                button("=", "equals"),
                button("%", "operator")
            )

            var index = 0
            for (row in 0 until 6) {
                for (col in 0 until 4) {
                    if (index < buttons.size) {
                        add(buttons[index], col, row)
                        index++
                    }
                }
            }
        }

        return VBox(grid).apply {
            style = "-fx-padding: 16px;"
            maxWidth = Double.MAX_VALUE
            alignment = Pos.CENTER_RIGHT
        }
    }

    private fun button(text: String, styleClass: String): Button {
        return Button(text).apply {
            this.styleClass.add("button")
            this.styleClass.add(styleClass)
            setOnAction { handleButtonClick(text) }
        }
    }

    private fun handleButtonClick(value: String) {
        when (value) {
            in "0123456789" -> viewModel.inputDigit(value[0])
            "." -> viewModel.inputDecimal()
            "C" -> viewModel.clear()
            "+", "-", "*", "/", "%" -> viewModel.setOperator(value)
            "=" -> viewModel.onEqualsPressed()
            "sin", "cos", "tan" -> viewModel.applyTrigFunction(value)
            "BIN" -> viewModel.convertToBase(2)
            "HEX" -> viewModel.convertToBase(16)
            "XOR" -> viewModel.startXor()
        }
        updateDisplay()
    }
    private fun updateDisplay() {
        val newText = viewModel.displayText
        displayLabel.text = newText

        if (viewModel.isDisplayingError) {
            displayLabel.styleClass.add("error")
        } else {
            displayLabel.styleClass.remove("error")
        }

        javafx.application.Platform.runLater {
            if (displayLabel.width > display.viewportBounds.width) {
                display.hvalue = 0.0
            } else {
                display.hvalue = 1.0
            }
        }
    }

}