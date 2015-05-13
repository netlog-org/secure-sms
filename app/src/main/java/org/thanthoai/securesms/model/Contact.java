package org.thanthoai.securesms.model;

import java.util.HashMap;
import java.util.Map;

public class Contact {
    public String DisplayName;
    public String PrimaryNumber;
    public CharSequence SpannablePrimaryNumber;
    public Map<String, String> PhoneNumbers = new HashMap<>();

    public Contact() {

    }

    public Contact(Contact o) {
        DisplayName = o.DisplayName;
        PrimaryNumber = o.PrimaryNumber;
        PhoneNumbers = o.PhoneNumbers;
    }
}
