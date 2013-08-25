package fr.nicopico.dashclock.birthday;

import fr.nicopico.dashclock.birthday.data.Birthday;
import fr.nicopico.dashclock.birthday.data.BirthdayRetriever;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/**
 * User: Nicolas PICON
 * Date: 24/08/13 - 17:22
 */
public class BirthdayService extends DashClockExtension {

    public static final String PREF_DAYS_LIMIT_KEY = "pref_days_limit";

    private BirthdayRetriever birthdayRetriever;
    private int daysLimit;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        birthdayRetriever = new BirthdayRetriever();
        updatePreferences();
    }

    @SuppressWarnings("ConstantConditions")
    private void updatePreferences() {
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        daysLimit = Integer.valueOf(sharedPreferences.getString(PREF_DAYS_LIMIT_KEY, "7"));
    }

    @Override
    protected void onUpdateData(int reason) {
        final List<Birthday> birthdays = birthdayRetriever.getContactWithBirthdays(getApplicationContext());
        final Resources res = getResources();

        if (reason == UPDATE_REASON_SETTINGS_CHANGED) {
            updatePreferences();
        }

        if (birthdays.size() > 0) {
            Birthday birthday = birthdays.get(0);
            DateTime today = new DateTime();

            DateTime birthdayEvent = birthday.birthdayDate.toDateTime(today);
            int days;
            if (birthdayEvent.isAfter(today)) {
                days = Days.daysBetween(today, birthdayEvent).getDays();
            }
            else {
                // Next birthday event is next year
                days = Days.daysBetween(today, birthdayEvent.plusYears(1)).getDays();
            }
            if (days > daysLimit) {
                publishUpdate(new ExtensionData().visible(false));
                return;
            }

            StringBuilder body = new StringBuilder();

            // Age
            if (!birthday.unknownYear) {
                int age = today.get(DateTimeFieldType.year()) - birthday.year;
                body.append(res.getString(R.string.age_format, age));
                body.append(' ');
            }

            // When
            int daysFormatResId;
            switch (days) {
            case 0:
                daysFormatResId = R.string.when_today_format;
                break;
            case 1:
                daysFormatResId = R.string.when_tomorrow_format;
                break;
            default:
                daysFormatResId = R.string.when_days_format;
            }
            body.append(res.getString(daysFormatResId, days));

            // Open contact info on click
            Intent contactIntent = new Intent(Intent.ACTION_VIEW);
            //noinspection ConstantConditions
            contactIntent.setData(Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_URI, String.valueOf(birthday.contactId)
            ));

            // Display message
            publishUpdate(
                    new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_extension_white)
                            .status(birthday.displayName)
                            .expandedTitle(res.getString(R.string.title_format, birthday.displayName))
                            .expandedBody(body.toString())
                            .clickIntent(contactIntent)
            );
        }
        else {
            // No upcoming birthday
            publishUpdate(new ExtensionData().visible(false));
        }
    }
}
