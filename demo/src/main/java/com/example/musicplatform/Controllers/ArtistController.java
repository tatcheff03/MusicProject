package com.example.musicplatform.Controllers;


import com.example.musicplatform.Data.Artist;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.musicplatform.Services.ArtistService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("artists")
public class ArtistController {
    private final ArtistService artistService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Artist> createArtist(
            @RequestPart("data") Artist artist,
            @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto) throws IOException {
        return ResponseEntity.ok(artistService.createArtist(artist, profilePhoto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateArtist(
            @PathVariable String id,
            @RequestPart("data") Artist artist,
            @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto) throws IOException {
        return artistService.updateArtist(id, artist, profilePhoto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Artist>> getAllArtists() {
        return ResponseEntity.ok(artistService.getAllArtists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArtistById(@PathVariable String id) {
        return artistService.getArtistById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable String id) {
        artistService.deleteArtist(id);
        return ResponseEntity.noContent().build();
    }
}


