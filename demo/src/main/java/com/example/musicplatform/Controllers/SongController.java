package com.example.musicplatform.Controllers;


import com.example.musicplatform.Data.Song;
import com.example.musicplatform.Services.S3Service;
import com.example.musicplatform.Services.SongService;
import lombok.AllArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@RestController
@AllArgsConstructor
@RequestMapping("/songs")
public class SongController {
    private final SongService songService;
    private final S3Service s3Service;

    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody Song song) {
        Song createdSong = songService.createSong(song);
        return ResponseEntity.ok(createdSong);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Song> uploadSong(
            @RequestPart("file") MultipartFile file,
            @RequestPart("cover") MultipartFile coverArt,
            @RequestPart("metadata") Song songMetadata) throws IOException {

        String s3Key = s3Service.uploadFile(file);  // Upload file to S3
        songMetadata.setS3Url(s3Key);                // Set the S3 file key in metadata
        String coverKey = s3Service.uploadCoverArt(coverArt); // Upload cover art to S3
        songMetadata.setCoverArtUrl(coverKey); // Set the cover art URL in metadata

        Song savedSong = songService.createSong(songMetadata);
        return ResponseEntity.ok(savedSong);
    }

    @GetMapping
    public ResponseEntity<List<Song>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSong(
            @PathVariable String id,
            @RequestBody Song updatedSong) {

        return songService.updateSongMetadata(id, updatedSong)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSongMultipart(
            @PathVariable String id,
            @RequestPart("metadata") Song updatedSong,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "cover", required = false) MultipartFile coverArt) {
        try {
            Optional<Song> updated = songService.updateSongWithFiles(id, updatedSong, file, coverArt);
            return updated
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error uploading files: " + e.getMessage());
        }
    }



    @GetMapping("/{id}")
    public ResponseEntity<?> getSongById(@PathVariable String id) {
        return songService.getSongById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable String id) {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }

}

