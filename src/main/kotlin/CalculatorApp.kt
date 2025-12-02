import javafx.application.Application

import javafx.stage.Stage
import javafx.scene.Scene

class CalculatorApp : Application() {
    override fun start(stage: Stage) {
        val viewModel = CalculatorViewModel()
        val view = CalculatorView(viewModel)

        stage.title = "Calculator"
        stage.scene = Scene(view.createRoot(), 300.0, 400.0)
        stage.show()
    }
}

fun main() {
    Application.launch(CalculatorApp::class.java)
}