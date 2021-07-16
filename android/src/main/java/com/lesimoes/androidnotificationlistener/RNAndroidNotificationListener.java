 
package com.lesimoes.androidnotificationlistener;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RNAndroidNotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    // here are three varaibles that one for notification , number and type off the number we get form notificcation
    private static final String TIMEANDDATENOTIFICATION = "TIMEANDDATENOTIFICATION";
    private static final String PREVIOUS_INCOMMING_NUMBER = "PREVIOUS_INCOMMING_NUMBER";
    private static final String PREVIOUS_INCOMMING_NUMBER_TYPE ="PREVIOUS_INCOMMING_NUMBER_TYPE";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (notification == null || notification.extras == null) return;
        }

        String app = sbn.getPackageName();
        boolean isDefault = isDefaultDialer(getApplicationContext(),app);

        if (app == null) app = "Unknown";

        CharSequence titleChars = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            titleChars = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        }

        CharSequence textChars = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            textChars = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }

        if (titleChars == null || textChars == null) return;
        
        String title = titleChars.toString();
        String text = textChars.toString();

        if (text == null || "".equals(text) || title == null || "".equals(title)) return;
        Log.e("Notification_Details", text + "  " + title + " " + app);
        if(isDefault) {
            // getting the current sytem time and date

            Long currenttimestamp = (Long) (System.currentTimeMillis());

            String previoustimestamp = TinyDB.getInstance(getApplicationContext()).getString(TIMEANDDATENOTIFICATION);
            String previous_incomming_number = TinyDB.getInstance(getApplicationContext()).getString(PREVIOUS_INCOMMING_NUMBER);
            String previous_incomming_number_type = TinyDB.getInstance(getApplicationContext()).getString(PREVIOUS_INCOMMING_NUMBER_TYPE);
            //  check condition of the app is install for first time the the pervios timestamp is empty
            if(previoustimestamp.isEmpty()) {
                previoustimestamp = String.valueOf(currenttimestamp);
            }
            if(previous_incomming_number.isEmpty()){
                previous_incomming_number="";
            }

            if(previous_incomming_number_type.isEmpty()){
                previous_incomming_number_type ="";
            }


            long time_difference =  Math.abs(Long.parseLong(previoustimestamp) - currenttimestamp);


            String current_incomming_number = titleChars.toString();  // mobile number
            String current_incomming_number_type = textChars.toString();  // call type
            /*
             when a user recieve a full call then its ringing duration os for 30 sec. so after ever 30 seconds
             we we revice a notification form the default dialer wehn will send that notification
            */
            if(current_incomming_number_type.contains("dialing")){
                Log.e("Time Difference", "time Differnce" + time_difference);
                if(time_difference == 0){
                    sendNotifcation(app , currenttimestamp, current_incomming_number,current_incomming_number_type);
                }else if(time_difference  > 500) {
                    sendNotifcation(app, currenttimestamp, current_incomming_number, current_incomming_number_type);
                }
            }else if(time_difference == 0 ){
                if(current_incomming_number.contains(("missed"))){
                    Log.e("Notification", "Notification Show 1");
                    sendNotifcation(app , currenttimestamp, current_incomming_number_type,current_incomming_number);
                }else{
                    Log.e("Notification", "Notification Show 2");
                    sendNotifcation(app ,currenttimestamp, current_incomming_number, current_incomming_number_type);
                }
            }else if((time_difference/1000) > 30){
                // Here is a call for notification

                if(current_incomming_number.toLowerCase().contains(("missed"))){
                    Log.e("Notification", "Notification Show 3 ");
                    sendNotifcation(app , currenttimestamp, current_incomming_number_type,current_incomming_number);
                }else{
                    Log.e("Notification", "Notification Show 4");
                    sendNotifcation(app ,currenttimestamp, current_incomming_number, current_incomming_number_type);
                }
             }else if(time_difference < 800 && time_difference > 2){
            // IGNORE NOTIFICATION
                Log.e("Notification", "Notification IGNORE");

             } else if(previous_incomming_number_type.toLowerCase().contains("incoming") && current_incomming_number_type.toLowerCase().contains(("incoming"))){
                /// show Notification
                Log.e("Notification", "Notification Show 5");
                sendNotifcation(app ,currenttimestamp, current_incomming_number, current_incomming_number_type);
            }else if(previous_incomming_number_type.toLowerCase().contains("missed") && current_incomming_number.toLowerCase().contains("missed")){
                Log.e("Notification", "Notification Show 6");
                sendNotifcation(app , currenttimestamp, current_incomming_number_type,current_incomming_number);
            }else{
              Log.e("Notification", "Notification Ignore");
            }
        }
        else{
            // not a default notification so just  igone this
            return;
        }

    }

    public void sendNotifcation (String app , Long currenttimestamp, String Number , String type ) {
      if(type.toLowerCase().contains("ongoing")){

      }else {
          TinyDB.getInstance(getApplicationContext()).putString(TIMEANDDATENOTIFICATION, String.valueOf(currenttimestamp));
          TinyDB.getInstance(getApplicationContext()).putString(PREVIOUS_INCOMMING_NUMBER, String.valueOf(Number));
          TinyDB.getInstance(getApplicationContext()).putString(PREVIOUS_INCOMMING_NUMBER_TYPE, String.valueOf(type));

          Intent serviceIntent = new Intent(getApplicationContext(), RNAndroidNotificationListenerHeadlessJsTaskService.class);
          serviceIntent.putExtra("app", app);
          serviceIntent.putExtra("Number", Number);
          serviceIntent.putExtra("type", type);
          HeadlessJsTaskService.acquireWakeLockNow(getApplicationContext());
          getApplicationContext().startService(serviceIntent);
          WritableMap params = Arguments.createMap();
          params.putString("app", app);
          params.putString("Number", Number);
          params.putString("type", type);
          //  Log.e(TAG, "Notification if condition 3  same number: " + app + " | " + current_incomming_number + " | " + current_incomming_number_type);
          RNAndroidNotificationListenerModule.sendEvent("notificationReceived", params);
      }
    }

    public boolean isDefaultDialer(Context context, String packageNameToCheck) {
        Intent dialingIntent = new Intent(Intent.ACTION_DIAL).addCategory(Intent.CATEGORY_DEFAULT);
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(dialingIntent, 0);
        if (resolveInfoList.size() < 1)
            return false;
        else
        {
            if(packageNameToCheck.contains("incallui")) return true;
            for (ResolveInfo item : resolveInfoList)
            {
                if(packageNameToCheck.equals(item.activityInfo.packageName))
                {
                    return true;
                }
            }
        }
        return packageNameToCheck == resolveInfoList.get(0).activityInfo.packageName;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}