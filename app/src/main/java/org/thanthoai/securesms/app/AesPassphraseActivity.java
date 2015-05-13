package org.thanthoai.securesms.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.thanthoai.securesms.R;
import org.thanthoai.securesms.crypto.AESHelper;
import org.thanthoai.securesms.fragments.DoneCancelBarFragment;
import org.thanthoai.securesms.model.Contact;
import org.thanthoai.securesms.model.PassphraseModel;
import org.thanthoai.securesms.services.UpdatePassphraseService;
import org.thanthoai.securesms.utils.CacheHelper;
import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.Keys;

import java.util.List;

public class AesPassphraseActivity extends BasePassphraseActivity {

    private String mAddress, mAppPassphrase;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAddress = getIntent().getStringExtra(Keys.ADDRESS);
        mAppPassphrase = getIntent().getStringExtra(Keys.APP_PASSPHRASE);

        final DoneCancelBarFragment frag = (DoneCancelBarFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_done_cancel);
        frag.setMode(DoneCancelBarFragment.MODE_NAVIGATION);
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

        if (mAddress != null) {
            if (PassphraseModel.findByAddress(this, mAddress) == null) {
                editOld.setText(mAppPassphrase);
                editOld.setVisibility(View.GONE);
            }
            frag.setDropdownItems(new String[]{mAddress});
        } else {
            List<Contact> contacts = (List<Contact>) CacheHelper.getInstance().get("contact");
            String[] items = new String[contacts.size()];
            for (int i = 0; i < items.length; i++) {
                items[i] = contacts.get(i).DisplayName;
            }
            frag.setDropdownItems(items);
            frag.setCancelIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        }
    }

    @Override
    protected void onUpdatePassphraseDone(Intent data) {
        super.onUpdatePassphraseDone(data);
        final boolean ok = data.getBooleanExtra("result", false);
        int resId = (ok) ? R.string.update_passphrase_success
                : R.string.update_passphrase_failure;
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();

        data.putExtra("passphrase", editNew1.getText().toString());
        setResult(ok ? RESULT_OK : RESULT_CANCELED, data);
        if (mAddress != null) finish();
    }

    private void onDone() {
        if (!checkFieldValid()) return;

        final String textOld = editOld.getText().toString();
        final String textNew = editNew1.getText().toString();

        if (checkCurrentPassphraseValid(mAddress, textOld, mAppPassphrase)) {
            Intent i = new Intent(this, UpdatePassphraseService.class);
            i.setAction(UpdatePassphraseService.ACTION_UPDATE_AES_PASSPHRASE);
            i.putExtra(Keys.ADDRESS, mAddress);
            i.putExtra(Keys.OLD_PASSPHRASE, textOld);
            i.putExtra(Keys.NEW_PASSPHRASE, textNew);
            i.putExtra(Keys.APP_PASSPHRASE, mAppPassphrase);
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
}
