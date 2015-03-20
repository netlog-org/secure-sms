package org.anhtn.securesms.model;

public class SmsMessage {

    public static final int TYPE_INBOX = 1;
    public static final int TYPE_SENT = 2;

    public int Id;
    public int Type;
    public String Content;
    public String Date;
}