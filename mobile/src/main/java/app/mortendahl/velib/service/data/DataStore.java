package app.mortendahl.velib.service.data;

import android.content.Context;
import android.os.Environment;

import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import app.mortendahl.velib.Logger;
import app.mortendahl.velib.network.jcdecaux.Position;
import app.mortendahl.velib.service.guiding.SetDestinationEvent;

public final class DataStore {

    private static Context appContext;
    private static String filename;

    private DataStore() {}

    public static void configure(Context appContext) {

        DataStore.appContext = appContext;

        try {

            //File storageDir = appContext.getFilesDir();  // private storage
            File storageDir = Environment.getExternalStorageDirectory();  // public storage
            filename = storageDir.getCanonicalPath() + "/eventstore.json";
            //filename = "eventstore.json";

            Logger.info(Logger.TAG_SYSTEM, DataStore.class, "storing events in " + filename);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

    }

    public static synchronized void record(BaseEvent event) {

        if (event == null) { return; }

        Writer w = null;
        try {

            String serialisedEvent = event.toJson().toString();

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

    public static ArrayList<SuggestedDestination> getSortedSuggestedDestinations() {

    }

    public static void updateSuggestedDestinations(ArrayList<SuggestedDestination> suggestedDestinations) {
    }
}
