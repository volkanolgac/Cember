package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java tools.ZipHelper <srcDir> <outputZip>");
            System.exit(1);
        }
        File srcDir = new File(args[0]);
        File zipFile = new File(args[1]);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipDirectory(srcDir, srcDir, zos);
        }
        System.out.println("✅ Created ZIP archive: " + zipFile.getAbsolutePath() + " (" + (zipFile.length() / 1024) + " KB)");
    }

    private static void zipDirectory(File rootDir, File currentDir, ZipOutputStream zos) throws Exception {
        File[] files = currentDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(rootDir, file, zos);
            } else {
                String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zos.putNextEntry(zipEntry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }
}
