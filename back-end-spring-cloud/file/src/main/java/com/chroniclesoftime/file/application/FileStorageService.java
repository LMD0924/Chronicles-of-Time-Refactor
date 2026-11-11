package com.chroniclesoftime.file.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class FileStorageService {
    @Value("${cot.storage.dir:./uploads}")
    private String baseDir;

    public Map<String, Object> store(MultipartFile file, String kind) throws IOException {
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        LocalDate d = LocalDate.now();
        Path rel = Paths.get(kind, String.valueOf(d.getYear()), String.format("%02d", d.getMonthValue()),
                String.format("%02d", d.getDayOfMonth()));
        Path dir = Paths.get(baseDir).toAbsolutePath().normalize().resolve(rel);
        Files.createDirectories(dir);
        Path target = dir.resolve(name);
        file.transferTo(target.toFile());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fileId", name);
        m.put("fileName", original);
        m.put("size", file.getSize());
        m.put("contentType", file.getContentType());
        m.put("url", "/files/" + rel.toString().replace('\\', '/') + "/" + name);
        return m;
    }

    public boolean remove(String fileId) {
        try (var stream = Files.walk(Paths.get(baseDir).toAbsolutePath().normalize())) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileId))
                    .findFirst().map(p -> { try { return Files.deleteIfExists(p); } catch (IOException e) { return false; } })
                    .orElse(false);
        } catch (IOException e) { return false; }
    }
}
