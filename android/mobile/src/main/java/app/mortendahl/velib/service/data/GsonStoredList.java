package app.mortendahl.velib.service.data;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import app.mortendahl.velib.Logger;

public abstract class GsonStoredList<T> extends BaseStoredList<T> {

    private final Gson gson;
    private final Type type;

    public GsonStoredList(String id) {
        super(id);
        this.gson = new Gson();
        this.type = getType();
    }

    @Override
    protected T convertFromLine(String json) {
        T item = gson.fromJson(json, type);
        Logger.debug(Logger.TAG_GUI, this, item.getClass().getCanonicalName());
        return item;
    }

    @Override
    protected String convertToLine(T item) {
        return gson.toJson(item);
    }

    protected abstract Type getType();

}
