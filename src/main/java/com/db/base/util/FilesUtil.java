package com.db.base.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FilesUtil {

    /**
     * 递归获取指定目录下所有文件（非文件夹）的路径
     * @param rootDir 根目录路径
     * @return 所有文件的规范路径列表
     */
    public static List<String> getAllFilePaths(String rootDir) {
        List<String> filePaths = new ArrayList<>();
        Path rootPath = Paths.get(rootDir);

        // 检查根目录是否有效
        if (!Files.exists(rootPath)) {
            System.err.println("目录不存在：" + rootDir);
            return filePaths;
        }
        if (!Files.isDirectory(rootPath)) {
            System.err.println("不是目录：" + rootDir);
            return filePaths;
        }

        try {
            // 遍历目录树（深度无限制）
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                // 访问文件时，添加其规范路径
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 确保是普通文件（非目录、非符号链接等）
                    if (attrs.isRegularFile()) {
                        filePaths.add(file.toRealPath().toString()); // 规范化路径
                    }
                    return FileVisitResult.CONTINUE;
                }

                // 目录不可访问时跳过
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("无法访问路径：" + file + "，原因：" + exc.getMessage());
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException e) {
            System.err.println("遍历目录失败：" + e.getMessage());
        }

        return filePaths;
    }

}
