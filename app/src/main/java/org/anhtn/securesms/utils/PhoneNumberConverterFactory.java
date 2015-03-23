package org.anhtn.securesms.utils;

import java.util.Locale;

public class PhoneNumberConverterFactory {

    public static IPhoneNumberConverter getConverter(Locale locale) {
        if (locale.getCountry().equals(Country.VIETNAM)) {
            return new VietNamPhoneNumberConverter();
        }
        throw new RuntimeException("Phone number converter for this locale not exist");
    }

    /**
     * Converter for VietNam
     */
    private static class VietNamPhoneNumberConverter implements IPhoneNumberConverter {

        @Override
        public String toLocal(String phoneNumber) {
            phoneNumber = phoneNumber.replace("+84", "0");
            if (phoneNumber.startsWith("84")) {
                phoneNumber = phoneNumber.substring(2);
                phoneNumber = "0" + phoneNumber;
            }
            return phoneNumber;
        }

        @Override
        public String toGlobal(String phoneNumber) {
            if (phoneNumber.startsWith("0")) {
                phoneNumber = phoneNumber.substring(1);
            }
            phoneNumber = "+84" + phoneNumber;
            return phoneNumber;
        }

        @Override
        public boolean isValid(String phoneNumber) {
            return phoneNumber.length() >= 9;
        }
    }
}