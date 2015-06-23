package app.mortendahl.velib.library.eventbus;

import android.content.Context;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import app.mortendahl.velib.Logger;
import de.greenrobot.event.EventBus;

public final class EventSystem {

    private static Context appContext;
    private static String filename;

    private EventSystem() {}

    public static void configure(Context appContext) {

        EventSystem.appContext = appContext;

        try {

            //File storageDir = appContext.getFilesDir();  // private storage
            File storageDir = Environment.getExternalStorageDirectory();  // public storage
            filename = storageDir.getCanonicalPath() + "/eventstore.json";
            //filename = "eventstore.json";

            Logger.info(Logger.TAG_SYSTEM, EventSystem.class, "storing events in " + filename);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

    }

    public static void post(Object event) {
        EventBus.getDefault().post(event);
    }

    public static void post(BaseEvent event) {
        store(event);
        EventBus.getDefault().post(event);
    }

    public static synchronized void store(BaseEvent event) {

        if (event == null) { return; }

        Writer w = null;
        try {

            String serialisedEvent = event.toJson().toString();

            w = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            w.write(serialisedEvent + "\n");
            w.flush();

        }
        catch (Exception e) {
            Logger.error(Logger.TAG_SYSTEM, EventSystem.class, e);
        }
        finally {
            if (w != null) {
                try { w.close(); }
                catch (IOException e) {}
                w = null;
            }
        }

    }

    public static List<JSONObject> loadAll() {

        ArrayList<JSONObject> events = new ArrayList<>();

        BufferedReader r = null;
        try {

            r = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = r.readLine()) != null) {

                try {
                    JSONObject jsonEvent = new JSONObject(line);
                    events.add(jsonEvent);
                }
                catch (JSONException e) {
                    Logger.error(Logger.TAG_SYSTEM, EventSystem.class, e);
                }

            }

        } catch (Exception e) {
            Logger.error(Logger.TAG_SYSTEM, EventSystem.class, e);
        }
        finally {
            if (r != null) {
                try { r.close(); }
                catch (IOException e) {}
                r = null;
            }
        }

        return events;

    }

    public static boolean isRegistered(Object subscriber) {
        return EventBus.getDefault().isRegistered(subscriber);
    }

    public static void register(Object subscriber) {
        EventBus.getDefault().register(subscriber);
    }

    public static void unregister(Object subscriber) {
        EventBus.getDefault().unregister(subscriber);
    }
}
