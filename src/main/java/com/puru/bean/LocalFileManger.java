package com.puru.bean;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class LocalFileManger {

    public void deleteFile(String filePath) {

        File file = new File(filePath);
        try {
            if (isExists(file.getPath())) {
                file.delete();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not delete file : " + filePath, e);
        }
    }

    public void moveFile(String sourcePath, String targetPath) {
        try {
            Files.move(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to move file:" + sourcePath + "with exception:" + e);
        }

    }

    public boolean isExists(String filePath) {

        return new File(filePath).exists();
    }

    public String getAbsolutePath(String filepath) {

        if (filepath == null || filepath.isEmpty() || filepath.isBlank()) {
            return filepath;
        }

        System.out.println("**** filepath::" + filepath);

        if (new File(filepath).exists()) {
            return filepath;
        }
        try {
            URL resource = this.getClass().getClassLoader().getResource(filepath);
            if (resource == null) {
                return filepath;
            }
            return Paths.get(resource.toURI()).toFile().getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Unable to find absolute path for : " + filepath, e);
        }
    }


    public BufferedReader getBufferedReader(String filepath) throws Exception {
        return new BufferedReader(new InputStreamReader(getInputStream(filepath)));
    }

    public InputStream getInputStream(String filepath) throws Exception {

        final File file = new File(filepath);
        if (file.exists()) {

            InputStream inputStream = new FileInputStream(file);
            return inputStream;
        }
        try {
            final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(filepath);
            return resourceAsStream;
        } catch (Exception e) {
            throw new RuntimeException("Unable to find absolute path for : " + filepath, e);
        }
    }

    public List<InputStream> getInputStreams(final String directory, final String extension, final boolean recursive)
            throws Exception {

        try {
            final File dir = new File(directory);
            if (dir.exists() && dir.isDirectory()) {

                Collection<File> files = FileUtils.listFiles(dir, new String[]{extension}, recursive);
                List<InputStream> inputStreamList = new ArrayList<>(files.size());
                for (File file : files) {
                    InputStream inputStream = new FileInputStream(file);
                    inputStreamList.add(inputStream);
                }
                return inputStreamList;
            }

            final Resource[] resources = getResources(directory, extension, recursive);
            List<InputStream> inputStreamList = new ArrayList<>(resources.length);
            for (Resource resource : resources) {
                inputStreamList.add(resource.getInputStream());
            }
            return inputStreamList;

        } catch (Exception e) {
            throw new RuntimeException("Unable to find absolute path for : " + directory, e);
        }
    }

    private Resource[] getResources(final String directory, final String extension, final boolean recursive)
            throws Exception {

        ClassLoader classLoader = this.getClass().getClass().getClassLoader();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        String classPath;

        if (StringUtils.isEmpty(directory) || directory.startsWith(File.separator)) {
            throw new IllegalArgumentException("Directory cannot be empty/null or start with /");
        }

        if (directory.endsWith(File.separator)) {
            classPath = "classpath:" + directory;
        } else {
            classPath = "classpath:" + directory + File.separator;
        }

        if (recursive) {
            classPath = classPath + "**" + File.separator;
        }

        if (StringUtils.isEmpty(extension)) {

            classPath = classPath + "*.*";

        } else if (extension.startsWith(".")) {

            classPath = classPath + "*" + extension;

        } else {

            classPath = classPath + "*." + extension;

        }
        return resolver.getResources(classPath);
    }

    public void createFile(final String fileName) {

        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            return;
        }

        if (file.exists()) {
            deleteFile(fileName);
        }

        try {
            Files.createFile(Path.of(fileName));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create file at : " + fileName, e);
        }
    }

    public void createFolder(final String folderName) {

        File folder = new File(folderName);
        if (folder.exists() && folder.isDirectory()) {
            return;
        }

        if (folder.exists()) {
            deleteFile(folderName);
        }

        try {
            Files.createDirectory(Path.of(folderName));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create folder at : " + folderName, e);
        }
    }

    public void removeFolder(final String folderName, boolean suppressErrors) {

        try {
            FileUtils.deleteDirectory(new File(folderName));
        } catch (IOException e) {
            if (suppressErrors) {
                return;
            }
            throw new RuntimeException("Unable to delete folder :" + folderName, e);
        }
    }

    public String[] listFiles(final String folderName) {

        File folder = new File(folderName);
        if (folder.exists()) {
            return folder.list();
        }
        return null;
    }

    public List<String> readFileLineByLine(String file) throws IOException {

        List<String> list = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = getBufferedReader(file);
            String line = reader.readLine();
            while (line != null) {
                list.add(line);
                line = reader.readLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read file[" + file + "]", e);
        } finally {
            if (reader != null)
                reader.close();
        }
        return list;
    }

    public boolean removeFirstLineFromFile(String file) throws IOException {

        BufferedReader reader = null;
        BufferedWriter writer = null;
        String fileName = null;
        try {

            int lastIndex = file.lastIndexOf(File.separator);

            fileName = "temp_" + file.substring(lastIndex + 1);

            if (lastIndex > -1) {
                fileName = file.substring(0, lastIndex) + File.separator + fileName;
            }
            reader = getBufferedReader(file);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(fileName))));
            String line = reader.readLine();
            //Ignore the first line
            if (line != null) {
                line = reader.readLine();
            }
            while (line != null) {
                if (StringUtils.isNotEmpty(line)) {
                    writer.write(line);
                }
                line = reader.readLine();
                if (line != null)
                    writer.newLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read file[" + file + "]", e);
        } finally {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (fileName != null) {
                moveFile(fileName, file);
            }
        }
        return true;
    }
}
