package org.thanthoai.securesms.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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

import java.util.ArrayList;
import java.util.List;

public class AesPassphraseActivity extends BasePassphraseActivity {

    static final int MODE_SINGLE = 1;
    static final int MODE_MULTIPLE = 2;

    private String mAppPassphrase;
    private int mMode;
    private final List<String> mAddresses = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DoneCancelBarFragment frag = (DoneCancelBarFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_done_cancel);

        frag.setMode(DoneCancelBarFragment.MODE_NAVIGATION);
        frag.setOnDoneListener(new DoneCancelBarFragment.OnDoneClickListener() {
            @Override
            public void onClick(View v) {
                if (updatePassphrase(mAddresses.get(frag.getSpinner().getSelectedItemPosition()))) {
                    pd.show();
                }
                clearFields();
            }
        });
        frag.setOnCancelListener(new DoneCancelBarFragment.OnCancelClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        frag.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiWhenAddressChanged(mAddresses.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String address = getIntent().getStringExtra(Keys.ADDRESS);
        mAppPassphrase = getIntent().getStringExtra(Keys.APP_PASSPHRASE);
        mMode = (address != null) ? MODE_SINGLE : MODE_MULTIPLE;

        if (mMode == MODE_SINGLE) {
            frag.setDropdownItems(new String[]{address});
            mAddresses.add(address);
        } else {
            List<Contact> contacts = (List<Contact>) CacheHelper.getInstance().get("contact");
            List<String> items = new ArrayList<>();

            for (Contact contact : contacts) {
                if (contact.PhoneNumbers.size() == 1) {
                    items.add(contact.DisplayName);
                    mAddresses.add(contact.PrimaryNumber);
                } else {
                    for (String number : contact.PhoneNumbers.keySet()) {
                        items.add(String.format("%s (%s)", contact.DisplayName, number));
                        mAddresses.add(number);
                    }
                }
            }

            String[] itemArr = new String[items.size()];
            items.toArray(itemArr);

            frag.setDropdownItems(itemArr);
            frag.setCancelIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        }

        updateUiWhenAddressChanged(mAddresses.get(0));
    }

    @Override
    protected void onUpdatePassphraseDone(Intent data) {
        super.onUpdatePassphraseDone(data);

        final boolean ok = data.getBooleanExtra("result", false);
        int resId = (ok) ? R.string.update_passphrase_success
                : R.string.update_passphrase_failure;
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();

        if (mMode == MODE_SINGLE) {
            data.putExtra("passphrase", editNew1.getText().toString());
            setResult(ok ? RESULT_OK : RESULT_CANCELED, data);
            finish();
        }
    }

    private boolean updatePassphrase(String address) {
        if (!checkFieldValid()) return false;

        final String textOld = editOld.getText().toString();
        final String textNew = editNew1.getText().toString();

        if (checkCurrentPassphraseValid(address, textOld, mAppPassphrase)) {
            Intent i = new Intent(this, UpdatePassphraseService.class);
            i.setAction(UpdatePassphraseService.ACTION_UPDATE_AES_PASSPHRASE);
            i.putExtra(Keys.ADDRESS, address);
            i.putExtra(Keys.OLD_PASSPHRASE, textOld);
            i.putExtra(Keys.NEW_PASSPHRASE, textNew);
            i.putExtra(Keys.APP_PASSPHRASE, mAppPassphrase);
            startService(i);
            return true;
        } else {
            Toast.makeText(this, R.string.current_passphrase_incorrect,
                    Toast.LENGTH_SHORT).show();
            editOld.requestFocus();
        }

        return false;
    }

    private boolean checkCurrentPassphraseValid(String address, String passphrase, String appPassphrase) {
        PassphraseModel model = PassphraseModel.findByAddress(this, address);
        if (model != null) {
            final String passphraseInDb = AESHelper.decryptFromBase64(appPassphrase,
                    model.Passphrase.replace(Global.MESSAGE_PREFIX, ""));
            return passphraseInDb != null && passphraseInDb.equals(passphrase);
        }
        return true;
    }

    private void updateUiWhenAddressChanged(String address) {
        PassphraseModel model = PassphraseModel.findByAddress(AesPassphraseActivity.this, address);
        editOld.setVisibility(model != null ? View.VISIBLE : View.GONE);
    }
}
