package com.project.hsf.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUploadUtil {

    public static String saveFile(String uploadDir, MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate a random ID for the file to prevent duplicate names
        String originalFileName = multipartFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ioe) {
            throw new IOException("Could not save image file: " + fileName, ioe);
        }
    }
    
    public static void cleanDir(String dir) {
        Path dirPath = Paths.get(dir);

        try {
            Files.list(dirPath).forEach(file -> {
                if (!Files.isDirectory(file)) {
                    try {
                        Files.delete(file);
                    } catch (IOException ex) {
                        System.out.println("Could not delete file: " + file);
                    }
                }
            });
        } catch (IOException ex) {
            System.out.println("Could not list directory: " + dirPath);
        }
    }

    public static void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;
        
        // Convert URL to Path: /uploads/abc.jpg -> uploads/abc.jpg
        String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
        Path path = Paths.get(relativePath);
        
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("Deleted file: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Could not delete file: " + path + ". Error: " + e.getMessage());
        }
    }
}
