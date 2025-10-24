/**
 * This class receives the data decoded using NativeHelper + ffmpeg.
 * The data is then streamed using RTP to an external gStreamer node.
 * The destination IP and port number are stored in the settings.
 *
 * See: https://github.com/The1only/rosettadrone/issues/131
 *
 * Based on: https://github.com/DJI-Mobile-SDK-Tutorials/Android-VideoStreamDecodingSample
 * This implements the demoType == USE_SURFACE_VIEW_DEMO_DECODER
 *
 * JNI -> onDataRecv() -> splitNALs() -> sendNAL() -> H264Packetizer
 **/

package sq.rogue.rosettadrone.video;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class VideoService extends Service implements NativeHelper.NativeDataListener {

    private static final String TAG = VideoService.class.getSimpleName();
    protected H264Packetizer mPacketizer;
    protected Thread thread;
    private boolean isRunning = false;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private String mip;
    private int mvideoPort;
    private int mvideoBitrate = 3000;
    private int mencodeSpeed = 2;

    @Override
    public void onCreate() {
        Log.e(TAG, "oncreate Video ");

        if (mPacketizer != null && mPacketizer.getRtpSocket() != null)
            mPacketizer.getRtpSocket().close();

        mPacketizer = new H264Packetizer();
        initVideoStreamDecoder();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        setActionDroneDisconnected();
    }

    public class LocalBinder extends Binder {
        public VideoService getInstance() {
            return VideoService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelID = "video_service";
        String channelName = "RosettaDrone 3 Video Service";
        NotificationChannel chan = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelID;
    }

    public void setParameters(String ip, int videoPort, int videoBitrate, int encodeSpeed) {
        Log.e(TAG, "setParameters");
        mip = ip;
        mvideoPort = videoPort;
        mvideoBitrate = videoBitrate;
        mencodeSpeed = encodeSpeed;
        initPacketizer(mip, mvideoPort, mvideoBitrate, mencodeSpeed);
    }

    public void setDualVideo(boolean dualVideo) {
        mPacketizer.socket.UseDualVideo(dualVideo);
    }

    private void setActionDroneDisconnected() {
        stopForeground(true);
        isRunning = false;

        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private void initVideoStreamDecoder() {
        NativeHelper.getInstance().init();
        NativeHelper.getInstance().setDataListener(this);
    }

    private void initPacketizer(String ip, int videoPort, int videoBitrate, int encodeSpeed) {
        Log.i(TAG, "Gst initPacketizer. ");

        try {
            mPacketizer.getRtpSocket().setDestination(InetAddress.getByName(ip), videoPort, videoPort);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error setting destination for RTP packetizer", e);
        }

        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void splitNALs(byte[] buffer) {
        if (buffer == null || buffer.length == 0) return;

        if (isAnnexB(buffer)) {
            int start = 0;
            while (start < buffer.length) {
                int next = findNextStartCode(buffer, start + 3);
                int nalStart = skipStartCode(buffer, start, next);
                if (nalStart < next) {
                    emitNal(Arrays.copyOfRange(buffer, nalStart, next));
                }
                if (next == buffer.length) break;
                start = next;
            }
        } else {
            int offset = 0;
            while (offset + 4 <= buffer.length) {
                int nalSize = ((buffer[offset] & 0xFF) << 24)
                        | ((buffer[offset + 1] & 0xFF) << 16)
                        | ((buffer[offset + 2] & 0xFF) << 8)
                        | (buffer[offset + 3] & 0xFF);
                offset += 4;
                if (nalSize <= 0 || offset + nalSize > buffer.length) {
                    break;
                }
                emitNal(Arrays.copyOfRange(buffer, offset, offset + nalSize));
                offset += nalSize;
            }
        }
    }

    private boolean isAnnexB(byte[] buffer) {
        return buffer.length >= 4
                && buffer[0] == 0x00
                && buffer[1] == 0x00
                && buffer[2] == 0x00
                && buffer[3] == 0x01;
    }

    private int findNextStartCode(byte[] data, int index) {
        for (int i = index; i + 3 <= data.length; i++) {
            if (data[i] == 0x00 && data[i + 1] == 0x00) {
                if (data[i + 2] == 0x01) {
                    return i;
                }
                if (i + 3 < data.length && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                    return i;
                }
            }
        }
        return data.length;
    }

    private int skipStartCode(byte[] data, int start, int next) {
        int i = start;
        while (i < next && data[i] == 0x00) {
            i++;
        }
        if (i < next && data[i] == 0x01) {
            i++;
        }
        if (i < next && data[i] == 0x00 && (i + 1) < next && data[i + 1] == 0x01) {
            i += 2;
        }
        return i;
    }

    private void emitNal(byte[] packet) {
        if (packet == null || packet.length == 0) {
            return;
        }
        byte[] nal = new byte[packet.length + 4];
        nal[0] = 0x00;
        nal[1] = 0x00;
        nal[2] = 0x00;
        nal[3] = 0x01;
        System.arraycopy(packet, 0, nal, 4, packet.length);
        sendNAL(nal);
    }

    protected void sendNAL(byte[] buffer) {
        // Pack a single NAL for RTP and send
        if (mPacketizer != null) {
            mPacketizer.setInputStream(new ByteArrayInputStream(buffer));
            mPacketizer.run();
        }
    }

    @Override
    public void onDataRecv(byte[] data, int size, int frameNum, boolean isKeyFrame, int width, int height) {
        if (size > 0 && isRunning) {
            // Pack the raw H.264 stream...
            try {
                splitNALs(data);
            } catch (Exception e){
                Log.d("VideoService",Log.getStackTraceString(e));
            }
        }
    }
}
