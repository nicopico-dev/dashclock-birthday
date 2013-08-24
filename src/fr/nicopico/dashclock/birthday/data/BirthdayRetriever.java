package fr.nicopico.dashclock.birthday.data;

import org.joda.time.MonthDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

/**
 * User: Nicolas PICON
 * Date: 24/08/13 - 18:58
 */
public class BirthdayRetriever {

    private static final Pattern regexDate;
    static {
        try {
            regexDate = Pattern.compile("(\\d{4}|-)-(\\d{2})-(\\d{2})", Pattern.COMMENTS);
        }
        catch (PatternSyntaxException ex) {
            throw new Error(ex);
        }
    }

    public List<Birthday> getContactWithBirthdays(Context context) {
        ContentResolver contentResolver = context.getContentResolver();

        // Retrieve contacts with birthdays
        @SuppressWarnings("ConstantConditions")
        Cursor c = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                new String[] {
                        ContactsContract.Data.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Event.START_DATE
                },
                String.format(
                        "%s = ? and %s = %d",
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Event.TYPE,
                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                ),
                new String[] {
                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
                },
                ContactsContract.Data.CONTACT_ID
        );

        List<Birthday> result = new ArrayList<Birthday>(c != null ? c.getCount() : 0);
        try {
            while (c != null && c.moveToNext()) {
                result.add(buildBirthday(contentResolver, c));
            }
        }
        finally {
            if (c != null) c.close();
        }

        Collections.sort(result);
        return result;
    }

    private Birthday buildBirthday(ContentResolver contentResolver, Cursor c) {
        Birthday contact = new Birthday(contentResolver, c.getLong(0));

        // Analyze birthday string
        Matcher regexMatcher = regexDate.matcher(c.getString(1));
        if (regexMatcher.find()) {
            contact.birthdayDate = new MonthDay(
                    Integer.parseInt(regexMatcher.group(2)),
                    Integer.parseInt(regexMatcher.group(3))
            );

            if (!"-".equals(regexMatcher.group(1))) {
                contact.year = Integer.parseInt(regexMatcher.group(1));
                contact.unknownYear = false;
            }
        }

        return contact;
    }

}
