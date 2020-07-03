package com.tommihirvonen.exifnotes.activities

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.fragments.PreferenceFragment
import com.tommihirvonen.exifnotes.utilities.*

/**
 * PreferenceActivity contains the PreferenceFragment for editing the app's settings
 * and preferences.
 */
class PreferenceActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {

    companion object {
        /**
         * Public constant custom result code used to indicate that a database was imported
         */
        const val RESULT_DATABASE_IMPORTED = 0x10

        /**
         * Public constant custom result code used to indicate that the app's theme was changed
         */
        const val RESULT_THEME_CHANGED = 0x20
    }

    /**
     * Member to store the current result code to be passed to the activity which started
     * this activity for result.
     */
    var resultCode = 0x0
        set(value) {
            field = value
            setResult(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.enter_from_right, R.anim.hold)

        if (isAppThemeDark) {
            setTheme(R.style.AppTheme_Dark)
        }

        super.onCreate(savedInstanceState)

        // If the activity was recreated, get the saved result code
        savedInstanceState?.let { resultCode = it.getInt(ExtraKeys.RESULT_CODE) }

        setContentView(R.layout.activity_settings)
        setUiColor(true)
        supportActionBar?.title = resources.getString(R.string.Preferences)
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        prefs.registerOnSharedPreferenceChangeListener(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.rel_layout, PreferenceFragment()).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Save the result code so that it can be set for this activity's result when recreated
        super.onSaveInstanceState(outState)
        outState.putInt(ExtraKeys.RESULT_CODE, resultCode)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        setStatusBarColor(secondaryUiColor)
        setSupportActionBarColor(primaryUiColor)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right)
    }

}