package io.okhi.android_background_geofencing.models;

public class BackgroundGeofenceSetting {

    private boolean withForegroundService = false;

    private BackgroundGeofenceSetting(Builder builder) {
        this.withForegroundService = builder.withForegroundService;
    }

    public BackgroundGeofenceSetting () { }

    public static class Builder {
        private boolean withForegroundService = false;

        public Builder() { }

        public Builder setWithForegroundService(boolean withForegroundService) {
            this.withForegroundService = withForegroundService;
            return this;
        }

        public BackgroundGeofenceSetting build() {
            return new BackgroundGeofenceSetting(this);
        }
    }

    public boolean isWithForegroundService() {
        return withForegroundService;
    }

    public void setWithForegroundService(boolean withForegroundService) {
        this.withForegroundService = withForegroundService;
    }
}
