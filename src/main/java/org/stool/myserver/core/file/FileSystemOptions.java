package org.stool.myserver.core.file;

public class FileSystemOptions {


    public static final boolean DEFAULT_FILE_CACHING_ENABLED = false;
    public static final boolean DEFAULT_CLASS_PATH_RESOLVING_ENABLED = false;


    private boolean classPathResolvingEnabled = DEFAULT_CLASS_PATH_RESOLVING_ENABLED;
    private boolean fileCachingEnabled = DEFAULT_FILE_CACHING_ENABLED;

    public boolean isClassPathResolvingEnabled() {
        return classPathResolvingEnabled;
    }

    public void setClassPathResolvingEnabled(boolean classPathResolvingEnabled) {
        this.classPathResolvingEnabled = classPathResolvingEnabled;
    }

    public boolean isFileCachingEnabled() {
        return fileCachingEnabled;
    }

    public void setFileCachingEnabled(boolean fileCachingEnabled) {
        this.fileCachingEnabled = fileCachingEnabled;
    }
}
