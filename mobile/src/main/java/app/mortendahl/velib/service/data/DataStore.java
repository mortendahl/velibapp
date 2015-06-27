package app.mortendahl.velib.service.data;

import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import app.mortendahl.velib.Logger;

public final class DataStore {

    private static WeakHashMap<String, DataStore> cachedStores = new WeakHashMap<>();

    public static synchronized DataStore getCollection(String id) {
        DataStore store = cachedStores.get(id);
        if (store == null) {
            store = new DataStore(id);
            cachedStores.put(id, store);
        }
        return store;
    }

    private final String filename;

    private DataStore(String id) {

        try {

            File storageDir = Environment.getExternalStorageDirectory();  // public storage
            filename = storageDir.getCanonicalPath() + "/" + id + ".json";

            //File storageDir = appContext.getFilesDir();  // private storage
            //filename = "eventstore.json";

            Logger.info(Logger.TAG_SYSTEM, DataStore.class, "storing events in " + filename);

        } catch (Exception e) {
            throw new AssertionError(e);
        }

    }

    public synchronized void append(JsonFormattable object) {

        try {

            append(object.toJson());

        } catch (JSONException e) {
            Logger.error(Logger.TAG_SYSTEM, DataStore.class, e);
        }

    }

    public synchronized void append(JSONObject event) {

        if (event == null) { return; }

        Writer w = null;
        try {

            String serialisedEvent = event.toString();

            w = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            w.write(serialisedEvent + "\n");
            w.flush();

        }
        catch (Exception e) {
            Logger.error(Logger.TAG_SYSTEM, DataStore.class, e);
        }
        finally {
            if (w != null) {
                try { w.close(); }
                catch (IOException e) {}
                w = null;
            }
        }

    }

    public List<JSONObject> loadAll() {

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
                    Logger.error(Logger.TAG_SYSTEM, DataStore.class, e);
                }

            }

        } catch (Exception e) {
            Logger.error(Logger.TAG_SYSTEM, DataStore.class, e);
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

    public void replace(ArrayList<? extends JsonFormattable> collection) {

        Writer w = null;
        try {

            w = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));

            for (JsonFormattable object : collection) {
                String serialisedObject = object.toJson().toString();
                w.write(serialisedObject + "\n");
            }

            w.flush();

        }
        catch (Exception e) {
            Logger.error(Logger.TAG_SYSTEM, DataStore.class, e);
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
