package app.mortendahl.velib.library.background;

import android.content.Context;
import android.os.PowerManager;

import app.mortendahl.velib.Logger;

public class WakeLockManager {

    private static final long AUTOMATIC_WAKE_LOCK_RELEASE_DELAY = 1000 * 60 * 60;

    private PowerManager.WakeLock wakeLock = null;

    public synchronized void acquireWakeLock(Context appContext) {

        if (wakeLock == null) {
            Logger.debug(Logger.TAG_SERVICE, this, "acquireWakeLock, creating new wake lock");
            PowerManager powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BurstWakelock");
        }

        if (wakeLock.isHeld()) {
            Logger.debug(Logger.TAG_SERVICE, this, "acquireWakeLock, wake lock already held, skipping");
        } else {
            Logger.debug(Logger.TAG_SERVICE, this, "acquireWakeLock, acquiring wake lock");
            wakeLock.acquire(AUTOMATIC_WAKE_LOCK_RELEASE_DELAY);
        }

    }

    public synchronized void releaseWakelock() {

        if (wakeLock == null) {
            Logger.debug(Logger.TAG_SERVICE, this, "releaseWakelock, no wakelock to release, skipping");
        } else {
            boolean heldBefore = wakeLock.isHeld();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            boolean heldAfter = wakeLock.isHeld();
            Logger.debug(Logger.TAG_SERVICE, this, "releaseWakelock, releasing wake lock, held before:" + heldBefore + ", held after:" + heldAfter);
            if (heldAfter) { throw new AssertionError("wake lock not released as expected (still held; reference count?)"); }
        }

    }

}