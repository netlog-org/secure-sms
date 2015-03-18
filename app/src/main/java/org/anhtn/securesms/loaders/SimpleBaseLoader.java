package org.anhtn.securesms.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class SimpleBaseLoader<T> extends AsyncTaskLoader<T> {

    private T mData;

    public SimpleBaseLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(T data) {
        if (isReset()) {
            return;
        }
        T oldData = mData;
        mData = data;

        if (isStarted()) {
            super.deliverResult(data);
        }
        if (oldData != null && oldData != data) {
            oldData = null;
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }
        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    protected void setNewData(T data) {
        mData = data;
    }
}
