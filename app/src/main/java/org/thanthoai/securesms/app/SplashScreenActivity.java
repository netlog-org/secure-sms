package org.thanthoai.securesms.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.IntentCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import org.thanthoai.securesms.R;
import org.thanthoai.securesms.loaders.ContactLoader;
import org.thanthoai.securesms.model.Contact;
import org.thanthoai.securesms.model.DatabaseHandler;
import org.thanthoai.securesms.utils.CacheHelper;

import java.util.List;


public class SplashScreenActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<Contact>> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        if (!CacheHelper.getInstance().contains("contact")) {
            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            startSmsActivity();
        }
    }

    @Override
    public Loader<List<Contact>> onCreateLoader(int id, Bundle args) {
        return new ContactLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<Contact>> loader, List<Contact> data) {
        if (data != null) {
            CacheHelper.getInstance().put("contact", data);
        }
        initDatabase();
        startSmsActivity();
    }

    @Override
    public void onLoaderReset(Loader<List<Contact>> loader) {

    }

    private void startSmsActivity() {
        Intent i = new Intent(SplashScreenActivity.this, SmsConversationActivity.class);
        i.addFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void initDatabase() {
        DatabaseHandler db = new DatabaseHandler(this);
        db.getReadableDatabase();
        db.close();
    }
}
