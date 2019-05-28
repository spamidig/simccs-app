package simccs.dataStore;

import javafx.concurrent.Task;

import java.io.*;
import java.util.zip.*;

public class UnzipFilesTask extends Task<Void> {

    private String rootPath;
    private String fileName;

    public UnzipFilesTask(String rootPath, String fileName) {
        this.rootPath = rootPath;
        this.fileName = fileName;
    }

    @Override
    public Void call() throws InterruptedException {
        unzipFiles(rootPath, fileName);
        return null;
    }

    public long unzipFiles(String rootPath, String fileName) {
        try {
            final int BUFFER = 2048;
            BufferedOutputStream dest;

            ZipFile zFile = new ZipFile(rootPath + fileName);
            long numEntry = zFile.size();
            zFile.close();

            FileInputStream fis = new FileInputStream(rootPath + "/" + fileName);
            CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
            ZipEntry entry;
            long i=0;
            while((entry = zis.getNextEntry()) != null) {
                System.out.println("Extracting: " +entry);
                int fileNumber = Integer.parseInt(entry.getName().replaceAll("[^0-9]", ""));
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                new File(rootPath + "/run" + fileNumber).mkdirs();
                FileOutputStream fos = new FileOutputStream(rootPath + "/run" + fileNumber + "/" + entry.getName());
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
                i++;
                updateProgress(i, numEntry);
            }
            zis.close();
            System.out.println("Checksum: " + checksum.getChecksum().getValue());
            return i;
        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
