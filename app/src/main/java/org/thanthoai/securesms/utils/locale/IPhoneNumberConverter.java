package org.thanthoai.securesms.utils.locale;

@SuppressWarnings("unused")
public interface IPhoneNumberConverter {

    String toLocal(String phoneNumber);
    String toGlobal(String phoneNumber);
    boolean isValidPersonalNumber(String phoneNumber);

    class NotValidPersonalNumberException extends Exception {}
}
