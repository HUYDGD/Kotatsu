package org.koitharu.kotatsu.settings.backup

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.backup.DIR_BACKUPS
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import java.io.File

class PeriodicalBackupSettingsFragment : BasePreferenceFragment(R.string.periodic_backups),
	ActivityResultCallback<Uri?>, SharedPreferences.OnSharedPreferenceChangeListener {

	private val outputSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree(),
		this,
	)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_backup_periodic)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
		bindOutputSummary()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT -> outputSelectCall.tryLaunch(null)
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		settings.unsubscribe(this)
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			settings.periodicalBackupOutput = result
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT -> bindOutputSummary()
		}
	}

	private fun bindOutputSummary() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT) ?: return
		viewLifecycleScope.launch {
			preference.summary = withContext(Dispatchers.Default) {
				val value = settings.periodicalBackupOutput
				value?.toString() ?: preference.context.run {
					getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
				}.path
			}
		}
	}
}
