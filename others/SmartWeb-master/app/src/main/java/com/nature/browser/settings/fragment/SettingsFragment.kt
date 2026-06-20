package com.nature.browser.settings.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.nature.browser.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_headers)
    }
}