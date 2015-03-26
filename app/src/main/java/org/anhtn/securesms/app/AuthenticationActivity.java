package org.anhtn.securesms.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.anhtn.securesms.R;
import org.anhtn.securesms.fragments.DoneCancelBarFragment;
import org.anhtn.securesms.utils.Keys;
import org.anhtn.securesms.utils.PasswordManager;

public class AuthenticationActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        final EditText editPassword = (EditText) findViewById(R.id.edit_passphrase);
        final TextView textResult = (TextView) findViewById(R.id.text_error);

        final DoneCancelBarFragment frag = (DoneCancelBarFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_done_cancel);
        frag.setOnDoneListener(new DoneCancelBarFragment.OnDoneClickListener() {
            @Override
            public void onClick(View v) {
                final String passphrase = editPassword.getText().toString();
                if (passphrase.length() == 0) return;
                final boolean matched = PasswordManager.isPasswordMatched(
                        AuthenticationActivity.this, passphrase);
                if (matched) {
                    Intent i = new Intent();
                    i.putExtra(Keys.APP_PASSPHRASE, passphrase);
                    setResult(RESULT_OK, i);
                    finish();
                } else {
                    Toast.makeText(AuthenticationActivity.this,
                            R.string.current_passphrase_incorrect, Toast.LENGTH_SHORT).show();
                    editPassword.setText("");
                    textResult.setVisibility(View.VISIBLE);
                }
            }
        });
        frag.setOnCancelListener(new DoneCancelBarFragment.OnCancelClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        frag.setMode(DoneCancelBarFragment.MODE_NORMAL);
        frag.setTitleInNormalMode(R.string.enter_password);
    }
}
