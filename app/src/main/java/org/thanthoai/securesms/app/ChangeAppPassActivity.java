package org.thanthoai.securesms.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.thanthoai.securesms.R;
import org.thanthoai.securesms.fragments.DoneCancelBarFragment;
import org.thanthoai.securesms.services.UpdatePassphraseService;
import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.Keys;
import org.thanthoai.securesms.utils.AppPassphraseManager;

public class ChangeAppPassActivity extends BaseChangePassActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DoneCancelBarFragment frag = (DoneCancelBarFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_done_cancel);
        frag.setMode(DoneCancelBarFragment.MODE_NORMAL);
        frag.setTitleInNormalMode(R.string.pref_change_app_pass_title);
        frag.setOnDoneListener(new DoneCancelBarFragment.OnDoneClickListener() {
            @Override
            public void onClick(View v) {
                onDone();
            }
        });
        frag.setOnCancelListener(new DoneCancelBarFragment.OnCancelClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        if (!AppPassphraseManager.isExists(this)) {
            editOld.setVisibility(View.GONE);
            editOld.setText(Global.DEFAULT_PASSPHRASE);
        }
    }

    @Override
    protected void onUpdatePassphraseDone(Intent data) {
        super.onUpdatePassphraseDone(data);
        if (data.getBooleanExtra("result", false)) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void onDone() {
        if (!checkFieldValid()) return;

        final String textOld = editOld.getText().toString();
        final String textNew = editNew1.getText().toString();

        if (AppPassphraseManager.isMatched(this, textOld)) {
            Intent i = new Intent(this, UpdatePassphraseService.class);
            i.setAction(UpdatePassphraseService.ACTION_UPDATE_APP_PASSPHRASE);
            i.putExtra(Keys.OLD_PASSPHRASE, textOld);
            i.putExtra(Keys.NEW_PASSPHRASE, textNew);
            startService(i);

            pd.show();
        } else {
            Toast.makeText(this, R.string.current_passphrase_incorrect,
                    Toast.LENGTH_SHORT).show();
            editOld.setText("");
            editOld.requestFocus();
        }
    }
}
