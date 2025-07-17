package com.example.musicplatform.Controllers;

import com.example.musicplatform.Data.Playlist;
import com.example.musicplatform.Services.PlaylistService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@AllArgsConstructor
@RequestMapping("/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Playlist> createPlaylist(
            @RequestPart("data") Playlist playlist,
            @RequestPart(value = "coverArt", required = false) MultipartFile coverArt) throws IOException {
        return ResponseEntity.ok(playlistService.createPlaylist(playlist, coverArt));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePlaylist(
            @PathVariable String id,
            @RequestPart("data") Playlist playlist,
            @RequestPart(value = "coverArt", required = false) MultipartFile coverArt) throws IOException {
        return playlistService.updatePlaylist(id, playlist, coverArt)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<?> getAllPlaylists() {
        return ResponseEntity.ok(playlistService.getAllPlaylists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlaylistById(@PathVariable String id) {
        return playlistService.getPlaylistById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable String id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.noContent().build();
    }
}
