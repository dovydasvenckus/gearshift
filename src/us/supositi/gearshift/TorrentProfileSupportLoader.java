package us.supositi.gearshift;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;

public class TorrentProfileSupportLoader extends AsyncTaskLoader<TorrentProfile[]> {
    private TorrentProfile[] mProfiles;
    
    private OnSharedPreferenceChangeListener mListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            TorrentListActivity.logD("TPLoader: the pref of a profile has changed.");
            onContentChanged();
        }
        
    };
    
    
    private OnSharedPreferenceChangeListener mDefaultListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            TorrentListActivity.logD("Detault prefs changed " + key);
            if (key.equals(TorrentProfile.PREF_PROFILES)) {
                TorrentListActivity.logD("TPLoader: the pref 'profiles' has changed.");
                onContentChanged();
            }
        }
    };

    public TorrentProfileSupportLoader(Context context) {
        super(context);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(mDefaultListener);
    }

    @Override
    public TorrentProfile[] loadInBackground() {
        TorrentProfile[] profiles = TorrentProfile.readProfiles(getContext().getApplicationContext());
        
        TorrentListActivity.logD("TPLoader: Read {0} profiles", new Object[] {profiles.length});
        
        for (TorrentProfile prof : profiles) {
            SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences(
                    TorrentProfile.PREF_PREFIX + prof.getId(), Activity.MODE_PRIVATE);
            prefs.registerOnSharedPreferenceChangeListener(mListener);
        }

        return profiles;
    }

    @Override
    public void deliverResult(TorrentProfile[] profiles) {
        if (isReset()) {
            if (profiles != null) {
                onReleaseResources(profiles);
                return;
            }
        }
        
        TorrentProfile[] oldProfiles = mProfiles;
        mProfiles = profiles;
        
        if (isStarted()) {
            TorrentListActivity.logD("TPLoader: Delivering results: {0} profiles", new Object[] {profiles.length});
            super.deliverResult(profiles);
        }
        
        if (oldProfiles != null) {
            for (TorrentProfile prof : oldProfiles) {
                for (TorrentProfile newProf : mProfiles) {
                    if (!prof.getId().equals(newProf.getId())) {
                        SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences(
                                TorrentProfile.PREF_PREFIX + prof.getId(), Activity.MODE_PRIVATE);
                        prefs.unregisterOnSharedPreferenceChangeListener(mListener);
                    }
                }
            }
        }
    }
    
    @Override
    public void onCanceled(TorrentProfile[] profiles) {
        super.onCanceled(profiles);
        
        onReleaseResources(profiles);
    }
    
    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        
        TorrentListActivity.logD("TPLoader: onStartLoading()");
        
        if (mProfiles != null)
            deliverResult(mProfiles);
        
        if (takeContentChanged() || mProfiles == null) {
            TorrentListActivity.logD("TPLoader: forceLoad()");
            forceLoad();
        }
    }
    
    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        
        TorrentListActivity.logD("TPLoader: onStopLoading()");
        cancelLoad();
    }
    
    @Override
    protected void onReset() {
        super.onReset();
        
        TorrentListActivity.logD("TPLoader: onReset()");
        
        onStopLoading();
        
        if (mProfiles != null) {
            onReleaseResources(mProfiles);
            mProfiles = null;
        }
    }
    
    protected void onReleaseResources(TorrentProfile[] profiles) {
        for (TorrentProfile prof : profiles) {
            SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences(
                    TorrentProfile.PREF_PREFIX + prof.getId(), Activity.MODE_PRIVATE);
            prefs.unregisterOnSharedPreferenceChangeListener(mListener);
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        prefs.unregisterOnSharedPreferenceChangeListener(mDefaultListener);
    }
}
