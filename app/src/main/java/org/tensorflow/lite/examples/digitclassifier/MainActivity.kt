package org.tensorflow.lite.examples.digitclassifier

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.divyanshu.draw.widget.DrawView

class MainActivity : AppCompatActivity() {

  private var drawView: DrawView? = null
  private var clearButton: Button? = null
  private var clearResButton: Button? = null
  private var predictedTextView: TextView? = null
  private var resultTextView: TextView? = null
  private var digitClassifier = DigitClassifier(this)

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.tfe_dc_activity_main)

    // Setup view instances
    drawView = findViewById(R.id.draw_view)
    drawView?.setStrokeWidth(70.0f)
    drawView?.setColor(Color.WHITE)
    drawView?.setBackgroundColor(Color.BLACK)
    clearButton = findViewById(R.id.clear_button)
    clearResButton = findViewById(R.id.clear_res_button)
    predictedTextView = findViewById(R.id.predicted_text)
    resultTextView = findViewById(R.id.result_text)

    clearButton?.visibility = View.GONE
    // Setup clear drawing button
    clearButton?.setOnClickListener {
      drawView?.clearCanvas()
      predictedTextView?.text = getString(R.string.tfe_dc_prediction_text_placeholder)
    }

    clearResButton?.setOnClickListener{
      resultTextView?.text = "Result: "
    }

    // Setup classification trigger so that it classify after every stroke drew
    drawView?.setOnTouchListener { _, event ->
      // As we have interrupted DrawView's touch event,
      // we first need to pass touch events through to the instance for the drawing to show up
      drawView?.onTouchEvent(event)

      // Then if user finished a touch event, run classification
      if (event.action == MotionEvent.ACTION_UP) {
        classifyDrawing()
      }

      true
    }

    // Setup digit classifier
    digitClassifier
      .initialize()
      .addOnFailureListener { e -> Log.e(TAG, "Error to setting up digit classifier.", e) }
  }

  override fun onDestroy() {
    digitClassifier.close()
    super.onDestroy()
  }

  private fun classifyDrawing() {
    val bitmap = drawView?.getBitmap()

    if ((bitmap != null) && (digitClassifier.isInitialized)) {
      digitClassifier
        .classifyAsync(bitmap)
        .addOnSuccessListener {
            resultText ->

            predictedTextView?.text = getOutputString(resultText.first, resultText.second)
            if (resultText.second > 0.95) {
              resultTextView?.text = "%s%d".format(resultTextView?.text, resultText.first)
            } else {
              Toast.makeText(applicationContext, "Confident not high enough", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
          predictedTextView?.text = getString(
            R.string.tfe_dc_classification_error_message,
            e.localizedMessage
          )
          Log.e(TAG, "Error classifying drawing.", e)
        }
    }

    drawView?.clearCanvas()
  }

  private fun getOutputString(digit: Int, confident: Float): String {
    return "Prediction Result: %d\nConfidence: %2f".format(digit, confident)
  }


  companion object {
    private const val TAG = "MainActivity"
  }
}

