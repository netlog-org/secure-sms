package org.anhtn.securesms.setting;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;

import org.anhtn.securesms.R;
import org.anhtn.securesms.app.AesPassphraseActivity;
import org.anhtn.securesms.app.AppPassphraseActivity;
import org.anhtn.securesms.app.AuthenticationActivity;
import org.anhtn.securesms.services.UpdatePassphraseService;
import org.anhtn.securesms.utils.Global;
import org.anhtn.securesms.utils.Keys;
import org.anhtn.securesms.utils.PasswordManager;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final int AUTHENTICATE_CHANGE_AES_PASSPHRASE_CODE = 2609;
    public static final int AUTHENTICATE_DISABLE_APP_PASSPHRASE_CODE = 2810;
    public static final int CREATE_NEW_APP_PASSPHRASE_CODE = 1109;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        loadPrefFromXml();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @Nullable Preference preference) {
        assert preference != null;
        final boolean appPassphraseEnabled = preferenceScreen.getSharedPreferences()
                .getBoolean(Keys.PREF_ENABLE_APP_PASSPHRASE, false);

        if (Keys.PREF_CHANGE_APP_PASSPHRASE.equalsIgnoreCase(preference.getKey())) {
            Intent i = new Intent(getActivity(), AppPassphraseActivity.class);
            startActivity(i);
        }
        else if (Keys.PREF_CHANGE_AES_PASSPHRASE.equalsIgnoreCase(preference.getKey())) {
            if (appPassphraseEnabled) {
                Intent i = new Intent(getActivity(), AuthenticationActivity.class);
                startActivityForResult(i, AUTHENTICATE_CHANGE_AES_PASSPHRASE_CODE);
            } else {
                startAesPassphraseActivity(Global.DEFAULT_PASSPHRASE);
            }
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTHENTICATE_CHANGE_AES_PASSPHRASE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startAesPassphraseActivity(data.getStringExtra(Keys.APP_PASSPHRASE));
            }
            return;
        } else if (requestCode == AUTHENTICATE_DISABLE_APP_PASSPHRASE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent i = new Intent(getActivity(), UpdatePassphraseService.class);
                i.setAction(UpdatePassphraseService.ACTION_UPDATE_APP_PASSPHRASE);
                i.putExtra(Keys.OLD_PASSPHRASE, data.getStringExtra(Keys.APP_PASSPHRASE));
                i.putExtra(Keys.NEW_PASSPHRASE, Global.DEFAULT_PASSPHRASE);
                getActivity().startService(i);
            } else {
                rollBackPref(Keys.PREF_ENABLE_APP_PASSPHRASE, true);
            }
        } else if (requestCode == CREATE_NEW_APP_PASSPHRASE_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                rollBackPref(Keys.PREF_ENABLE_APP_PASSPHRASE, false);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Keys.PREF_ENABLE_APP_PASSPHRASE.equalsIgnoreCase(preference.getKey())) {
            if ((boolean) newValue) {
                if (!PasswordManager.hasPasswordSaved(getActivity())) {
                    Intent i = new Intent(getActivity(), AppPassphraseActivity.class);
                    startActivityForResult(i, CREATE_NEW_APP_PASSPHRASE_CODE);
                }
            } else {
                if (PasswordManager.hasPasswordSaved(getActivity())) {
                    Intent i = new Intent(getActivity(), AuthenticationActivity.class);
                    startActivityForResult(i, AUTHENTICATE_DISABLE_APP_PASSPHRASE_CODE);
                }
            }
            return true;
        }
        return false;
    }

    private void startAesPassphraseActivity(String appPassphrase) {
        Intent i = new Intent(getActivity(), AesPassphraseActivity.class);
        i.putExtra(Keys.APP_PASSPHRASE, appPassphrase);
        startActivity(i);
    }

    private void loadPrefFromXml() {
        addPreferencesFromResource(R.xml.pref_setting);
        findPreference(Keys.PREF_ENABLE_APP_PASSPHRASE).setOnPreferenceChangeListener(this);
    }

    private void rollBackPref(String key, boolean value) {
        SharedPreferences.Editor editor =
                getPreferenceScreen().getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.commit();

        getPreferenceScreen().removeAll();
        loadPrefFromXml();
    }
}
