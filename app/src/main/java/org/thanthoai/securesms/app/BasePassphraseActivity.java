package org.thanthoai.securesms.app;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.EditText;
import android.widget.Toast;

import org.thanthoai.securesms.R;
import org.thanthoai.securesms.services.UpdatePassphraseService;

public class BasePassphraseActivity extends ActionBarActivity {

    protected EditText editOld, editNew1, editNew2;
    protected ProgressDialog pd;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_passphrase);

        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.processing));
        pd.setCancelable(false);

        editOld = (EditText) findViewById(R.id.edit1);
        editNew1 = (EditText) findViewById(R.id.edit2);
        editNew2 = (EditText) findViewById(R.id.edit3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mUpdatePassphraseReceiver,
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

    protected boolean checkFieldValid() {
        final String textOld = editOld.getText().toString();
        final String textNew1 = editNew1.getText().toString();
        final String textNew2 = editNew2.getText().toString();

        if (textOld.length() == 0 || textNew1.length() == 0 || textNew2.length() == 0) {
            Toast.makeText(this, R.string.field_can_not_null, Toast.LENGTH_SHORT).show();
            editOld.requestFocus();
            return false;
        }
        if (!textNew1.equals(textNew2)) {
            Toast.makeText(this, R.string.passphrase_not_match, Toast.LENGTH_SHORT).show();
            editOld.requestFocus();
            return false;
        }
        return true;
    }

    @SuppressWarnings("unused")
    protected void onUpdatePassphraseDone(Intent data) {
        if (pd.isShowing()) pd.dismiss();
    }

    private BroadcastReceiver mUpdatePassphraseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdatePassphraseDone(intent);
        }
    };
}

