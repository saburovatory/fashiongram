package io.github.froger.instamaterial;

import android.app.Application;

import com.vk.sdk.VKSdk;

import timber.log.Timber;

public class InstaMaterialApplication extends Application {


    private String user;

    public void setUser(String user) {
        this.user = user;
    }

    public String getUSer() {
        return this.user;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        Timber.plant(new Timber.DebugTree());
    }
}
