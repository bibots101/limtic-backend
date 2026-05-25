package tn.limtic.limtic_backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public String storePhoto(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne peut pas être vide.");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path targetDir = Paths.get(uploadDir, subfolder).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(filename).normalize();
        file.transferTo(targetPath);

        return "/uploads/" + subfolder + "/" + filename;
    }
}
