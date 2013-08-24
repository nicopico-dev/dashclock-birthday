package fr.nicopico.dashclock.birthday;

import fr.nicopico.dashclock.birthday.data.Birthday;
import fr.nicopico.dashclock.birthday.data.BirthdayRetriever;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;
import org.joda.time.ReadableInstant;

import java.util.List;

import android.content.res.Resources;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/**
 * User: Nicolas PICON
 * Date: 24/08/13 - 17:22
 */
public class BirthdayService extends DashClockExtension {

    private BirthdayRetriever birthdayRetriever;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        birthdayRetriever = new BirthdayRetriever();
    }

    @Override
    protected void onUpdateData(int reason) {
        final List<Birthday> birthdays = birthdayRetriever.getContactWithBirthdays(getApplicationContext());
        final Resources res = getResources();

        if (birthdays.size() > 0) {
            Birthday birthday = birthdays.get(0);
            ReadableInstant today = new DateTime();

            StringBuilder body = new StringBuilder();

            // Age
            if (!birthday.unknownYear) {
                int age = today.get(DateTimeFieldType.year()) - birthday.year;
                body.append(res.getString(R.string.age_format, age));
                body.append(' ');
            }

            // In how many days
            int days = Days.daysBetween(today, birthday.birthdayDate.toDateTime(today)).getDays();
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

            // Display message
            publishUpdate(
                    new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.white_icon)
                            .status(res.getString(R.string.title_format, birthday.displayName))
                            .expandedBody(body.toString())
            );
        }
        else {
            // No upcoming birthday
            publishUpdate(new ExtensionData().visible(false));
        }
    }
}
