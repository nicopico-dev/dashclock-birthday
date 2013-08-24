package fr.nicopico.dashclock.birthday.data;

import org.joda.time.MonthDay;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/**
* User: Nicolas PICON
* Date: 24/08/13 - 19:09
*/
public class Birthday implements Comparable<Birthday> {
    public final long contactId;
    public final String displayName;
    public String uriPhoto;
    public String uriThumbnail;

    public MonthDay birthdayDate;
    public boolean unknownYear = true;
    public int year = 0;

    public final MonthDay TODAY = new MonthDay();

    public Birthday(ContentResolver contentResolver, long contactId) {
        this.contactId = contactId;

        // Retrieve contact details
        @SuppressWarnings("ConstantConditions")
        Cursor c = contentResolver.query(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, Uri.encode(String.valueOf(contactId))),
                new String[] {
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.Contacts.PHOTO_URI,
                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
                },
                null, null, null
        );

        try {
            if (c != null && c.moveToFirst()) {
                this.displayName = c.getString(1);
                this.uriPhoto = c.getString(2);
                this.uriThumbnail = c.getString(3);
            }
            else {
                this.displayName = "[UNKNOWN]";
            }
        }
        finally {
            if (c != null) c.close();
        }
    }

    @Override
    public int compareTo(Birthday another) {
        if (getSign(this.birthdayDate.compareTo(TODAY)) == getSign(another.birthdayDate.compareTo(TODAY))) {
            int birthdayCompare = this.birthdayDate.compareTo(another.birthdayDate);
            if (birthdayCompare != 0) {
                return birthdayCompare;
            }
            else {
                return this.displayName.compareTo(another.displayName);
            }
        }
        else {
            // this.birthday has passed -> display it last
            return this.birthdayDate.isBefore(TODAY) ? 1 : -1;
        }
    }

    /***
     * Get sign of value.
     * Main difference with <pre>Math.signum()</pre> is that 0 will return +1 too.
     * @param value integer
     * @return +1 if value is >= 0, else return -1
     */
    private int getSign(int value) {
        return value >= 0 ? 1 : -1;
    }
}
