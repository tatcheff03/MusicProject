package com.example.musicplatform.Controllers;


import com.example.musicplatform.Data.Album;
import com.example.musicplatform.Services.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
public class AlbumController {
    private final AlbumService albumService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Album> createAlbum
            (@RequestPart("data") Album album,
             @RequestPart(value = "coverArt", required = false) MultipartFile coverArt) throws IOException {
        Album created = albumService.createAlbum(album, coverArt);
        return ResponseEntity.ok(created);
    }

    // EDIT Album (patch, replace some fields, update cover optionally)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Album> updateAlbum(
            @PathVariable String id,
            @RequestPart("data") Album album,
            @RequestPart(value = "coverArt", required = false) MultipartFile coverArt) throws IOException {
        return albumService.updateAlbum(id, album, coverArt)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Album>> getAllAlbums() {
        List<Album> albums = albumService.getAllAlbums();
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Album> getAlbumById(@PathVariable String id) {
        return albumService.getAlbumById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE album by id (removes cover and auto-cleans S3 as in service)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable String id) {
        albumService.deleteAlbum(id);
        return ResponseEntity.noContent().build();
    }
}
