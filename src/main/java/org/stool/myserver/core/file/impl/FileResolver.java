package org.stool.myserver.core.file.impl;

import org.stool.myserver.core.file.FileSystemOptions;

import java.io.File;

public class FileResolver {

    private final File cwd;
    private File cacheDir;
    private Thread shutdwonHook;
    private final boolean enableCaching;
    private final boolean enableCpResolving;

    public FileResolver(FileSystemOptions fileSystemOptions) {
        this.enableCaching = fileSystemOptions.isFileCachingEnabled();
        this.enableCpResolving = fileSystemOptions.isClassPathResolvingEnabled();
        String cwdOverride = System.getProperty("vertx.cwd");
        if (cwdOverride != null) {
            cwd = new File(cwdOverride).getAbsoluteFile();
        } else {
            cwd = null;
        }
        if (this.enableCpResolving) {
            setupCacheDir();
        }
    }



    public File resolveFile(String fileName) {
        File file = new File(fileName);
        if (cwd != null && !file.isAbsolute()) {
            file = new File(cwd, fileName);
        }
        if (!this.enableCpResolving) {
            return file;
        }

        synchronized (this) {
            if (!file.exists()) {
                // Look for it in local file cache
                File cacheFile = new File(cacheDir, fileName);
                if (this.enableCaching && cacheFile.exists()) {
                    return cacheFile;
                }
            }
        }
        return file;
    }

    private void setupCacheDir() {

    }
}
