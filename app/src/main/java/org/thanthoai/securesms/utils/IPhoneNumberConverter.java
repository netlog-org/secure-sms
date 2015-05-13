package org.thanthoai.securesms.utils;

public interface IPhoneNumberConverter {

    public String toLocal(String phoneNumber);
    public String toGlobal(String phoneNumber);
    public boolean isValidPersonalNumber(String phoneNumber);

    public static class NotValidPersonalNumberException extends Exception {}
}
