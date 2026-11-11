package com.chroniclesoftime.file.interfaces;

import com.chroniclesoftime.file.application.FileStorageService;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileStorageService storage;
    public FileController(FileStorageService storage) { this.storage = storage; }

    @PostMapping("/image")
    public Result<?> image(@RequestParam("file") MultipartFile file) throws IOException {
        String ct = Optional.ofNullable(file.getContentType()).orElse("");
        if (!ct.startsWith("image/")) return Result.fail(400, "仅支持图片文件");
        return Result.ok(storage.store(file, "images"));
    }

    @PostMapping("/file")
    public Result<?> single(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(storage.store(file, "files"));
    }

    @PostMapping("/files")
    public Result<?> multiple(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        for (MultipartFile f : files) if (!f.isEmpty()) out.add(storage.store(f, "files"));
        return Result.ok(out);
    }

    @DeleteMapping("/{fileId}")
    public Result<?> delete(@PathVariable String fileId) {
        return Result.ok(Map.of("deleted", storage.remove(fileId)));
    }
}
