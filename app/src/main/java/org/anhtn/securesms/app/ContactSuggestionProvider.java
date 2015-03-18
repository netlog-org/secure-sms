package org.anhtn.securesms.app;

import android.content.SearchRecentSuggestionsProvider;

public class ContactSuggestionProvider extends SearchRecentSuggestionsProvider {

    public final static String AUTHORITY = "org.anhtn.securesms.ContactSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public ContactSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
