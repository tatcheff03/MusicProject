package com.example.musicplatform.Services;

import com.example.musicplatform.Data.Artist;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
@Service
@RequiredArgsConstructor

public class S3Service {
    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucketName;
    public static final Map<Character, String> CYRILLIC_TO_LATIN = Map.ofEntries(
            // Uppercase
            Map.entry('А', "A"), Map.entry('Б', "B"), Map.entry('В', "V"),
            Map.entry('Г', "G"), Map.entry('Д', "D"), Map.entry('Е', "E"),
            Map.entry('Ж', "Zh"), Map.entry('З', "Z"), Map.entry('И', "I"),
            Map.entry('Й', "Y"), Map.entry('К', "K"), Map.entry('Л', "L"),
            Map.entry('М', "M"), Map.entry('Н', "N"), Map.entry('О', "O"),
            Map.entry('П', "P"), Map.entry('Р', "R"), Map.entry('С', "S"),
            Map.entry('Т', "T"), Map.entry('У', "U"), Map.entry('Ф', "F"),
            Map.entry('Х', "H"), Map.entry('Ц', "Ts"), Map.entry('Ч', "Ch"),
            Map.entry('Ш', "Sh"), Map.entry('Щ', "Sht"), Map.entry('Ъ', "A"),
            Map.entry('Ь', "Y"), Map.entry('Ю', "Yu"), Map.entry('Я', "Ya"),
            // Lowercase
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"),
            Map.entry('г', "g"), Map.entry('д', "d"), Map.entry('е', "e"),
            Map.entry('ж', "zh"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('й', "y"), Map.entry('к', "k"), Map.entry('л', "l"),
            Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"),
            Map.entry('т', "t"), Map.entry('у', "u"), Map.entry('ф', "f"),
            Map.entry('х', "h"), Map.entry('ц', "ts"), Map.entry('ч', "ch"),
            Map.entry('ш', "sh"), Map.entry('щ', "sht"), Map.entry('ъ', "a"),
            Map.entry('ь', "y"), Map.entry('ю', "yu"), Map.entry('я', "ya")
    );

    // Transliterates Cyrillic to Latin before sanitizing
    private String transliterateAndSanitize(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(CYRILLIC_TO_LATIN.getOrDefault(c, String.valueOf(c)));
        }
        // Now run the regular sanitizer to remove any left-over unsafe chars
        return sanitizeS3KeyPart(sb.toString());
    }

    private String sanitizeS3KeyPart(String input) {
        return input.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio";
        String ext = "";
        if (original.lastIndexOf('.') != -1) {
            ext = original.substring(original.lastIndexOf('.'));
            original = original.substring(0, original.lastIndexOf('.'));
        }
        // >>> Use transliterateAndSanitize instead of just sanitizeS3KeyPart!
        String safeName = transliterateAndSanitize(original);
        String key = "songs/" + UUID.randomUUID() + "_" + safeName + ext;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return key;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return ""; // or throw an exception if you prefer
    }

    public String uploadCoverArt(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cover";
        String ext = "";
        if (original.lastIndexOf('.') != -1) {
            ext = original.substring(original.lastIndexOf('.'));
            original = original.substring(0, original.lastIndexOf('.'));
        }
        String safeName = sanitizeS3KeyPart(original) + ext;
        String key = "covers/" + UUID.randomUUID() + "_" + safeName;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return key;
    }


    public void uploadArtistMetadataJson(Artist artist) throws IOException {
        String key = "artist-profiles/" + artist.getId() + ".json";
        String json = new ObjectMapper().writeValueAsString(artist);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(json.getBytes(StandardCharsets.UTF_8)));
    }

    public String uploadArtistProfilePhoto(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "artist";
        String ext = "";
        if (original.lastIndexOf('.') != -1) {
            ext = original.substring(original.lastIndexOf('.'));
            original = original.substring(0, original.lastIndexOf('.'));
        }
        String safeName = sanitizeS3KeyPart(original) + ext;
        String key = "artist-profiles/" + UUID.randomUUID() + "_" + safeName;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return key;
    }

    public void copyFile(String sourceKey, String destKey) {
        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .bucket(bucketName)
                .copySource(bucketName + "/" + sourceKey)
                .key(destKey)
                .build();

        s3Client.copyObject(copyReq);

    }

    public String uploadFileToKey(MultipartFile file, String key) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return key;
    }

    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

}
