package com.dm.wallpaper.board.helpers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.dm.wallpaper.board.utils.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/*
 * Wallpaper Board
 *
 * Copyright (c) 2017 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class FileHelper {

    public static final String IMAGE_EXTENSION = ".jpeg";
    public static final int MB = (1024 * 1014);
    public static final int KB = 1024;

    public static long getCacheSize(@NonNull File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (File aFileList : fileList) {
                if (aFileList.isDirectory()) {
                    result += getCacheSize(aFileList);
                } else {
                    result += aFileList.length();
                }
            }
            return result;
        }
        return 0;
    }

    public static void clearCache(@NonNull File cache) {
        if (cache.isDirectory())
            for (File child : cache.listFiles())
                clearCache(child);
        cache.delete();
    }

    static boolean copyFile(@NonNull File file, @NonNull File target) {
        try {
            if (!target.getParentFile().exists()) {
                if (!target.getParentFile().mkdirs()) return false;
            }

            InputStream inputStream = new FileInputStream(file);
            OutputStream outputStream = new FileOutputStream(target);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            inputStream.close();
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (Exception e) {
            LogUtil.e(Log.getStackTraceString(e));
        }
        return false;
    }

    @Nullable
    public static Uri getUriFromFile(Context context, String applicationId, File file) {
        try {
            return FileProvider.getUriForFile(context, applicationId + ".fileProvider", file);
        } catch (IllegalArgumentException e) {
            LogUtil.e(Log.getStackTraceString(e));
        }
        return null;
    }
}
