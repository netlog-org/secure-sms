package org.anhtn.securesms.model;

import java.util.HashMap;
import java.util.Map;

public class ContactObject {
    public String DisplayName;
    public String PrimaryNumber;
    public Map<String, String> PhoneNumbers = new HashMap<>();
}
