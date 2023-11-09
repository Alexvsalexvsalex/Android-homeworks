package com.alexxx.coolcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.udojava.evalex.Expression
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ArithmeticException
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {
    private val PRECISION = 32

    private val EXPRESSION_KEY = "EXPRESSION"
    private val RESULT_KEY = "RESULT"

    private fun addToExpr(expr: String) {
        expressionView.text = expressionView.text.toString() + expr
    }

    private fun evaluateExpression() {
        val expr = expressionView.text.toString()
        try {
            resultView.text = String.format("%f", Expression(expr).setPrecision(PRECISION).eval())
        } catch (e: RuntimeException) {
            resultView.text = String.format(getString(R.string.error), e.localizedMessage)
        }
    }

    private fun copyToClipBoard(view: TextView) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("copied from calculator", view.text))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val addersMap = mapOf(
            num0Button to "0",
            num1Button to "1",
            num2Button to "2",
            num3Button to "3",
            num4Button to "4",
            num5Button to "5",
            num6Button to "6",
            num7Button to "7",
            num8Button to "8",
            num9Button to "9",
            plusButton to "+",
            minusButton to "-",
            mulButton to "*",
            divideButton to "/",
            percentButton to "%",
            dotButton to "."
        )
        for ((key, value) in addersMap) {
            key.setOnClickListener { addToExpr(value) }
        }

        resetButton.setOnClickListener { expressionView.text = "" }
        backspaceButton.setOnClickListener {
            val len_txt = expressionView.text.length
            if (len_txt > 0) {
                expressionView.text = expressionView.text.substring(0, len_txt - 1)
            }
        }
        calcButton.setOnClickListener { evaluateExpression() }

        expressionView.setOnClickListener { v -> copyToClipBoard(v as TextView) }
        resultView.setOnClickListener { v -> copyToClipBoard(v as TextView) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXPRESSION_KEY, expressionView.text.toString())
        outState.putString(RESULT_KEY, resultView.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        expressionView.text = savedInstanceState.getString(EXPRESSION_KEY)
        resultView.text = savedInstanceState.getString(RESULT_KEY)
    }
}