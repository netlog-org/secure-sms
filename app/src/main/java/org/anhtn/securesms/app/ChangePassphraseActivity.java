package org.anhtn.securesms.app;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.anhtn.securesms.R;
import org.anhtn.securesms.crypto.AESHelper;
import org.anhtn.securesms.model.PassphraseModel;
import org.anhtn.securesms.services.UpdatePassphraseService;
import org.anhtn.securesms.utils.Global;

public class ChangePassphraseActivity extends ActionBarActivity {

    private EditText editOld, editNew1, editNew2;
    private ProgressDialog pd;
    private String mAddress, mAppPassphrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_passphrase);

        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.processing));
        pd.setCancelable(false);

        editOld = (EditText) findViewById(R.id.edit1);
        editNew1 = (EditText) findViewById(R.id.edit2);
        editNew2 = (EditText) findViewById(R.id.edit3);

        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.button_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDone();
            }
        });

        mAddress = getIntent().getStringExtra("address");
        mAppPassphrase = getIntent().getStringExtra("app_passphrase");

        if (PassphraseModel.findByAddress(this, mAddress) == null) {
            editOld.setText(mAppPassphrase);
            editOld.setVisibility(View.GONE);
        }

        DoneCancelBarFragment frag = (DoneCancelBarFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_done_cancel);
        frag.setMode(DoneCancelBarFragment.MODE_NAVIGATION);
        frag.setDropdownItems(new String[]{mAddress});
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpdatePassphraseReceiver,
                new IntentFilter(UpdatePassphraseService.UPDATE_PASSPHRASE_DONE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mUpdatePassphraseReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pd.isShowing()) pd.dismiss();
    }

    private void onDone() {
        final String textOld = editOld.getText().toString();
        final String textNew1 = editNew1.getText().toString();
        final String textNew2 = editNew2.getText().toString();

        if (textOld.length() == 0 || textNew1.length() == 0 || textNew2.length() == 0) {
            Toast.makeText(this, R.string.field_can_not_null, Toast.LENGTH_SHORT).show();
            editOld.requestFocus();
            return;
        }
        if (!textNew1.equals(textNew2)) {
            Toast.makeText(this, R.string.passphrase_not_match, Toast.LENGTH_SHORT).show();
            editOld.requestFocus();
            return;
        }

        if (checkCurrentPassphraseValid(mAddress, textOld, mAppPassphrase)) {
            Intent i = new Intent(this, UpdatePassphraseService.class);
            i.setAction(Intent.ACTION_EDIT);
            i.putExtra("address", mAddress);
            i.putExtra("old_passphrase", textOld);
            i.putExtra("new_passphrase", textNew1);
            i.putExtra("app_passphrase", mAppPassphrase);
            startService(i);

            pd.show();
        } else {
            Toast.makeText(this, R.string.current_passphrase_incorrect,
                    Toast.LENGTH_SHORT).show();
            editOld.requestFocus();
        }
    }

    private boolean checkCurrentPassphraseValid(String address, String passphrase,
                                                String appPassphrase) {
        PassphraseModel model = PassphraseModel.findByAddress(this, address);
        if (model == null) return true;
        final String plainPassphrase = AESHelper.decryptFromBase64(appPassphrase,
                model.Passphrase.replace(Global.MESSAGE_PREFIX, ""));
        return plainPassphrase.equals(passphrase);
    }

    private BroadcastReceiver mUpdatePassphraseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (pd.isShowing()) pd.dismiss();
        }
    };
}
