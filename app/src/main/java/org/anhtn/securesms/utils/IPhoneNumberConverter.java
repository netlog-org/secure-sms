package org.anhtn.securesms.utils;

public interface IPhoneNumberConverter {

    public String toLocal(String phoneNumber);
    public String toGlobal(String phoneNumber);
    public boolean isValid(String phoneNumber);
}
