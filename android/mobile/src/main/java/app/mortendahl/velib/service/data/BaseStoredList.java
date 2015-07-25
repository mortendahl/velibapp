package app.mortendahl.velib.service.data;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import app.mortendahl.velib.Logger;

public abstract class BaseStoredList<T> {

    private final String filename;

    public BaseStoredList(String id) {

        try {

            File storageDir = Environment.getExternalStorageDirectory();  // public storage
            filename = storageDir.getCanonicalPath() + "/" + id + ".json";

            //File storageDir = appContext.getFilesDir();  // private storage
            //filename = "eventstore.json";

            Logger.info(Logger.TAG_SYSTEM, this, "storing events in " + filename);

        } catch (Exception e) {
            throw new AssertionError(e);
        }

    }

    protected abstract T convertFromLine(String json);

    protected abstract String convertToLine(T item);

    public synchronized List<T> getAll() {

        ArrayList<T> items = new ArrayList<>();

        BufferedReader r = null;
        try {

            r = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = r.readLine()) != null) {

                T item = convertFromLine(line);
                if (item != null) { items.add(item); }

            }

        } catch (IOException e) {
            Logger.error(Logger.TAG_SYSTEM, this, e);
        }
        finally {
            if (r != null) {
                try { r.close(); }
                catch (IOException e) {}
                r = null;
            }
        }

        return items;

    }

    public synchronized void append(T item) {

        if (item == null) { return; }

        Writer w = null;
        try {

            String json = convertToLine(item);

            w = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            w.write(json);
            w.write("\n");
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

    public synchronized void replace(Collection<? extends T> items) {

        Writer w = null;
        try {

            w = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));

            for (T item : items) {
                String json = convertToLine(item);
                w.write(json);
                w.write("\n");
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
