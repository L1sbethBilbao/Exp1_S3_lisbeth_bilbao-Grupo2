package com.duoc.empresa_transportista_efs.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.dto.GuiaMetadataDto;
import com.duoc.empresa_transportista_efs.exception.InvalidFileException;
import com.duoc.empresa_transportista_efs.exception.S3AccessDeniedException;
import com.duoc.empresa_transportista_efs.exception.S3BucketNotFoundException;
import com.duoc.empresa_transportista_efs.exception.S3ObjectNotFoundException;
import com.duoc.empresa_transportista_efs.exception.S3OperationException;
import com.duoc.empresa_transportista_efs.exception.S3UploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucket;

	public String getBucket() {
		return bucket;
	}

	public List<GuiaMetadataDto> listByPrefix(String prefix) {
		try {
			log.info("Listando objetos del bucket {} con prefijo: {}", bucket, prefix);

			ListObjectsV2Request request = ListObjectsV2Request.builder()
					.bucket(bucket)
					.prefix(prefix)
					.build();

			ListObjectsV2Response response = s3Client.listObjectsV2(request);

			return response.contents().stream()
					.filter(obj -> !obj.key().endsWith("/"))
					.map(obj -> new GuiaMetadataDto(
							obj.key(),
							obj.size(),
							obj.lastModified() != null ? obj.lastModified().toString() : null))
					.collect(Collectors.toList());

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("listar objetos del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al listar objetos del bucket: " + bucket, e);
		}
	}

	public byte[] downloadAsBytes(String key) {
		try {
			log.info("Descargando objeto: {} del bucket: {}", key, bucket);

			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build();

			ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);

			log.info("Objeto descargado exitosamente: {}", key);
			return responseBytes.asByteArray();

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (NoSuchKeyException e) {
			throw new S3ObjectNotFoundException(key, bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("descargar el objeto: " + key, e);
			}
			throw new S3OperationException("Error al descargar el objeto: " + key, e);
		}
	}

	public void upload(String key, MultipartFile file) {
		validateFile(file);

		try {
			log.info("Subiendo archivo: {} al bucket: {}, tamano: {} bytes", key, bucket, file.getSize());

			String contentType = file.getContentType() != null ? file.getContentType() : "application/pdf";

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(contentType)
					.contentLength(file.getSize())
					.build();

			s3Client.putObject(putObjectRequest,
					RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

			log.info("Archivo subido exitosamente: {}", key);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("subir archivo al bucket: " + bucket, e);
			}
			throw new S3UploadException("Error al subir el archivo a S3: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new S3UploadException("Error al leer el archivo: " + e.getMessage(), e);
		}
	}

	public void deleteObject(String key) {
		try {
			log.info("Eliminando objeto: {} del bucket: {}", key, bucket);

			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build();

			s3Client.deleteObject(deleteRequest);

			log.info("Objeto eliminado exitosamente: {}", key);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("eliminar objeto del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al eliminar el objeto: " + key, e);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidFileException("El archivo esta vacio o es nulo");
		}
		if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
			throw new InvalidFileException("El nombre del archivo no es valido");
		}
		if (file.getSize() == 0) {
			throw new InvalidFileException("El archivo no puede tener tamano 0");
		}
	}
}
