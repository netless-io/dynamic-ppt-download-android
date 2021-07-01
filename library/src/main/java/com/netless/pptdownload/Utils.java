package com.netless.pptdownload;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Utils {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    static void unzip(File zipFile, File targetDirectory) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    continue;
                }
                copyToFile(zis, file);
            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {

            }
        }
    }

    public static String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            int n;
            while (-1 != (n = isr.read(buffer))) {
                sb.append(buffer, 0, n);
            }
        }
        return sb.toString();
    }

    public static void writeStringToFile(String text, File file) throws IOException {
        try (OutputStreamWriter osr = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            osr.write(text);
        }
    }

    public static void copyToFile(InputStream inputStream, File file) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            int n;
            while (-1 != (n = inputStream.read(buffer))) {
                fos.write(buffer, 0, n);
            }
        }
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}
