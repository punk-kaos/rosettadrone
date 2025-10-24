package sq.rogue.rosettadrone;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import static sq.rogue.rosettadrone.util.TYPE_DRONE_ID;
import static sq.rogue.rosettadrone.util.TYPE_DRONE_RTL_ALTITUDE;
import static sq.rogue.rosettadrone.util.TYPE_FLIGHT_PATH_RADIUS;
import static sq.rogue.rosettadrone.util.TYPE_GCS_IP;
import static sq.rogue.rosettadrone.util.TYPE_GCS_PORT;
import static sq.rogue.rosettadrone.util.TYPE_VIDEO_BITRATE;
import static sq.rogue.rosettadrone.util.TYPE_VIDEO_IP;
import static sq.rogue.rosettadrone.util.TYPE_VIDEO_PORT;
import static sq.rogue.rosettadrone.util.TYPE_WAYPOINT_DISTANCE;
import static sq.rogue.rosettadrone.util.TYPE_WAYPOINT_MAX_ALTITUDE;
import static sq.rogue.rosettadrone.util.TYPE_WAYPOINT_MAX_SPEED;
import static sq.rogue.rosettadrone.util.TYPE_WAYPOINT_MIN_ALTITUDE;
import static sq.rogue.rosettadrone.util.TYPE_WAYPOINT_MIN_SPEED;
import static sq.rogue.rosettadrone.util.TYPE_WAYPOINT_TOTAL_DISTANCE;

public class NotificationHandler {
    private static final String TAG = NotificationHandler.class.getSimpleName();

    public static void notifySnackbar(View view, int resID, int duration) {
        if (view == null) {
            Log.e(TAG, "Skipping snackbar: anchor view is null");
            return;
        }
        if (resID == 0) {
            Log.e(TAG, "Skipping snackbar: invalid resource id 0", new IllegalArgumentException("resID=0"));
            return;
        }
        Snackbar.make(view, resID, duration).show();
    }

    public static void notifyAlert(Context context, int input, DialogInterface.OnClickListener clickListener,
                                   DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context, R.style.AppDialog);
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setOnCancelListener(cancelListener);
        switch (input) {
            case TYPE_GCS_IP:
                builder.setMessage(context.getResources().getString(R.string.error_gcs_ip));
                break;
            case TYPE_GCS_PORT:
                builder.setMessage(context.getResources().getString(R.string.error_gcs_port));
                break;
            case TYPE_VIDEO_IP:
                builder.setMessage(context.getResources().getString(R.string.error_video_ip));
                break;
            case TYPE_VIDEO_PORT:
                builder.setMessage(context.getResources().getString(R.string.error_video_port));
                break;
            case TYPE_DRONE_ID:
                builder.setMessage(context.getResources().getString(R.string.error_drone_id));
                break;
            case TYPE_DRONE_RTL_ALTITUDE:
                builder.setMessage(context.getResources().getString(R.string.error_rtl_altitude));
                break;
            case TYPE_VIDEO_BITRATE:
                builder.setMessage(context.getResources().getString(R.string.error_video_bitrate));
                break;
            case TYPE_FLIGHT_PATH_RADIUS:
                builder.setMessage(context.getResources().getString(R.string.error_flight_path_radius));
                break;
            case TYPE_WAYPOINT_MIN_ALTITUDE:
                builder.setMessage(context.getResources().getString(R.string.error_waypoint_min_altitude));
                break;
            case TYPE_WAYPOINT_MAX_ALTITUDE:
                builder.setMessage(context.getResources().getString(R.string.error_waypoint_max_altitude));
                break;
            case TYPE_WAYPOINT_DISTANCE:
                builder.setMessage(context.getResources().getString(R.string.error_waypoint_distance));
                break;
            case TYPE_WAYPOINT_TOTAL_DISTANCE:
                builder.setMessage(context.getResources().getString(R.string.error_waypoint_total_distance));
                break;
            case TYPE_WAYPOINT_MIN_SPEED:
                builder.setMessage(context.getResources().getString(R.string.error_waypoint_min_speed));
                break;
            case TYPE_WAYPOINT_MAX_SPEED:
                builder.setMessage(context.getResources().getString(R.string.error_waypoint_max_speed));
                break;
        }
        builder.show();
    }

}
