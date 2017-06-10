package io.github.froger.instamaterial;

import android.app.Application;

import com.vk.sdk.VKSdk;

import timber.log.Timber;

public class InstaMaterialApplication extends Application {
    private String user;
    private String fullName;

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return this.user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        Timber.plant(new Timber.DebugTree());
    }
}
