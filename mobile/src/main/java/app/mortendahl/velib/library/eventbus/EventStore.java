package app.mortendahl.velib.library.eventbus;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.VelibApplication;

public final class EventStore {

    private EventStore() {}

    private static Context appContext;
    private static String filename;

    public static void configure(Context appContext) {

        EventStore.appContext = appContext;

        try {

            //File storageDir = appContext.getFilesDir();  // private storage
            File storageDir = Environment.getExternalStorageDirectory();  // public storage
            filename = storageDir.getCanonicalPath() + "/eventstore.json";
            //filename = "eventstore.json";

            Logger.info(Logger.TAG_SYSTEM, EventStore.class, "storing events in " + filename);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

    }

    public static synchronized void storeEvent(BaseEvent event) {

        if (event == null) { return; }

        Writer w = null;
        try {

            String serialisedEvent = event.toJson().toString();

            w = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            w.write(serialisedEvent + "\n");
            w.flush();

        }
        catch (Exception e) {
            Logger.error(Logger.TAG_SYSTEM, EventStore.class, e.toString());
        }
        finally {
            if (w != null) {
                try { w.close(); }
                catch (IOException e) {}
                w = null;
            }
        }

    }

}
