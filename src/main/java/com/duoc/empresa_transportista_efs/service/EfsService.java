package com.duoc.empresa_transportista_efs.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EfsService {

	@Value("${efs.path}")
	private String efsPath;

	public File saveToEfs(String filename, MultipartFile multipartFile) throws IOException {
		Path dest = resolveFilePath(filename);
		Files.createDirectories(dest.getParent());
		multipartFile.transferTo(dest);
		log.info("Archivo guardado en EFS: {}", dest.toAbsolutePath());
		return dest.toFile();
	}

	public File saveBytes(String filename, byte[] content) throws IOException {
		Path dest = resolveFilePath(filename);
		Files.createDirectories(dest.getParent());
		Files.write(dest, content);
		log.info("Archivo guardado en EFS: {}", dest.toAbsolutePath());
		return dest.toFile();
	}

	public void deleteFile(String filename) throws IOException {
		Path filePath = resolveFilePath(filename).normalize();
		log.info("Intentando eliminar archivo EFS: {}", filePath);

		if (!Files.exists(filePath)) {
			log.warn("Archivo no encontrado en EFS: {}", filePath);
			return;
		}

		Files.delete(filePath);
		log.info("Archivo eliminado de EFS: {}", filePath);
	}

	private Path resolveFilePath(String filename) {
		String normalized = filename.replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		return Paths.get(efsPath, normalized.split("/"));
	}
}
