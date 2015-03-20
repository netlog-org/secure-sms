package org.anhtn.securesms.model;

import java.util.HashMap;
import java.util.Map;

public class ContactObject {
    public String DisplayName;
    public String PrimaryNumber;
    public CharSequence SpannablePrimaryNumber;
    public Map<String, String> PhoneNumbers = new HashMap<>();

    public ContactObject() {

    }

    public ContactObject(ContactObject o) {
        DisplayName = o.DisplayName;
        PrimaryNumber = o.PrimaryNumber;
        PhoneNumbers = o.PhoneNumbers;
    }
}
