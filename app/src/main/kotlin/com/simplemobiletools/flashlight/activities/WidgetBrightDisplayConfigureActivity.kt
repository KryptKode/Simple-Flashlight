package com.simplemobiletools.flashlight.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.FeatureLockedDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.IS_CUSTOMIZING_COLORS
import com.simplemobiletools.flashlight.R
import com.simplemobiletools.flashlight.extensions.config
import com.simplemobiletools.flashlight.helpers.MyWidgetBrightDisplayProvider
import kotlinx.android.synthetic.main.widget_bright_display_config.*

class WidgetBrightDisplayConfigureActivity : SimpleActivity() {
    private var mWidgetAlpha = 0f
    private var mWidgetId = 0
    private var mWidgetColor = 0
    private var mWidgetColorWithoutTransparency = 0
    private var mFeatureLockedDialog: FeatureLockedDialog? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_bright_display_config)
        initVariables()

        val isCustomizingColors = intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        config_save.setOnClickListener { saveConfig() }
        config_widget_color.setOnClickListener { pickBackgroundColor() }

        val primaryColor = getProperPrimaryColor()
        config_widget_seekbar.setColors(getProperTextColor(), primaryColor, primaryColor)

        if (!isCustomizingColors && !isOrWasThankYouInstalled()) {
            mFeatureLockedDialog = FeatureLockedDialog(this) {
                if (!isOrWasThankYouInstalled()) {
                    finish()
                }
            }
        }

        config_save.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
        config_save.setTextColor(getProperPrimaryColor().getContrastColor())
    }

    override fun onResume() {
        super.onResume()
        window.decorView.setBackgroundColor(0)
        setupToolbar(config_bright_display_toolbar)

        if (mFeatureLockedDialog != null && isOrWasThankYouInstalled()) {
            mFeatureLockedDialog?.dismissDialog()
        }
    }

    private fun initVariables() {
        mWidgetColor = config.widgetBgColor
        mWidgetAlpha = Color.alpha(mWidgetColor) / 255.toFloat()

        mWidgetColorWithoutTransparency = Color.rgb(Color.red(mWidgetColor), Color.green(mWidgetColor), Color.blue(mWidgetColor))
        config_widget_seekbar.setOnSeekBarChangeListener(seekbarChangeListener)
        config_widget_seekbar.progress = (mWidgetAlpha * 100).toInt()
        updateColors()
    }

    private fun saveConfig() {
        config.widgetBgColor = mWidgetColor
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mWidgetColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mWidgetColorWithoutTransparency = color
                updateColors()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetBrightDisplayProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateColors() {
        mWidgetColor = mWidgetColorWithoutTransparency.adjustAlpha(mWidgetAlpha)
        config_widget_color.setFillWithStroke(mWidgetColor, mWidgetColor)
        config_image.background.mutate().applyColorFilter(mWidgetColor)
    }

    private val seekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mWidgetAlpha = progress.toFloat() / 100.toFloat()
            updateColors()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
