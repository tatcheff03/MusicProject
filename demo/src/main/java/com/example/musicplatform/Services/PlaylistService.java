package com.example.musicplatform.Services;

import com.example.musicplatform.Data.Playlist;
import com.example.musicplatform.Data.Song;
import com.example.musicplatform.repo.PlaylistRepository;
import com.example.musicplatform.repo.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final S3Service s3Service;
    private final SongRepository songRepository;

    @Autowired
    public PlaylistService(PlaylistRepository playlistRepository, S3Service s3Service,SongRepository songRepository) {
        this.playlistRepository = playlistRepository;
        this.s3Service = s3Service;
        this.songRepository = songRepository;
    }
    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.'));
    }
    private String sanitizeS3KeyPart(String input) {
        return input.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }



    public Playlist createPlaylist(Playlist playlist, MultipartFile coverArt) throws IOException {
        // Sanitize playlist title for folder
        String safeTitle = sanitizeS3KeyPart(playlist.getTitle());
        String folderPrefix = "playlists/" + safeTitle + "/";

        // Cover art
        if (coverArt != null && !coverArt.isEmpty()) {
            String ext = getFileExtension(coverArt.getOriginalFilename());
            String safeCoverFilename = sanitizeS3KeyPart("cover" + ext);
            String key = folderPrefix + safeCoverFilename;
            s3Service.uploadFileToKey(coverArt, key);
            playlist.setCoverArtUrl(key);
        }

        // Song files
        if (playlist.getSongIds() != null && !playlist.getSongIds().isEmpty()) {
            List<String> songS3Keys = new ArrayList<>();
            for (String songId : playlist.getSongIds()) {
                songRepository.findById(songId).ifPresent(song -> {
                    String srcKey = song.getS3Url();
                    String baseName = srcKey.substring(srcKey.lastIndexOf('/') + 1);
                    String safeFilename = sanitizeS3KeyPart(baseName);
                    String destKey = folderPrefix + safeFilename;
                    try {
                        s3Service.copyFile(srcKey, destKey);
                        songS3Keys.add(destKey);
                    } catch (Exception e) {
                        System.err.println("ERROR: Could not copy " + srcKey + " -> " + destKey + ": " + e.getMessage());
                    }
                });
            }
            // playlist.setSongS3Keys(songS3Keys); // optional for convenience
        }

        return playlistRepository.save(playlist);
    }




    public Optional<Playlist> updatePlaylist(String id, Playlist update, MultipartFile coverArt) throws IOException {
        return playlistRepository.findById(id).map(existing -> {

            // Update fields if provided
            if (update.getTitle() != null) existing.setTitle(update.getTitle());
            if (update.getDescription() != null) existing.setDescription(update.getDescription());
            if (update.getOwnerId() != null) existing.setOwnerId(update.getOwnerId());

            // -- S3 FOLDER PREFIX (always use sanitized playlist title) --
            String safeTitle = sanitizeS3KeyPart(
                    (update.getTitle() != null) ? update.getTitle() : existing.getTitle()
            );
            String folderPrefix = "playlists/" + safeTitle + "/";
            System.out.println("FolderPrefix: " + folderPrefix);

            // -- COVER ART (also sanitized) --
            if (coverArt != null && !coverArt.isEmpty()) {
                String oldCover = existing.getCoverArtUrl();
                if (oldCover != null && !oldCover.isEmpty()) {
                    s3Service.deleteFile(oldCover);
                }
                String ext = getFileExtension(coverArt.getOriginalFilename());
                String safeCoverFilename = sanitizeS3KeyPart("cover" + ext);
                String key = folderPrefix + safeCoverFilename;
                try {
                    s3Service.uploadFileToKey(coverArt, key);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                existing.setCoverArtUrl(key);
            }

            // -- SONG S3 FILE MANAGEMENT --
            System.out.println("--- updatePlaylist start: " + id);
            if (update.getSongIds() != null) {
                List<String> oldIds = existing.getSongIds() == null
                        ? new ArrayList<>() : new ArrayList<>(existing.getSongIds());
                List<String> newIds = new ArrayList<>(update.getSongIds());
                System.out.println("oldIds: " + oldIds);
                existing.setSongIds(newIds);

                // Songs to add (in new, not in old)
                List<String> toAdd = new ArrayList<>(newIds);
                toAdd.removeAll(oldIds);
                System.out.println("Songs to add: " + toAdd);
                System.out.println("newIds: " + newIds);

                // Songs to remove (in old, not in new)
                List<String> toRemove = new ArrayList<>(oldIds);
                toRemove.removeAll(newIds);
                System.out.println("Songs to remove: " + toRemove);

                // Copy new songs to S3 playlist folder, using sanitized filenames
                for (String addId : toAdd) {
                    songRepository.findById(addId).ifPresent(song -> {
                        String srcKey = song.getS3Url();
                        String baseName = srcKey.substring(srcKey.lastIndexOf('/') + 1);
                        String safeFilename = sanitizeS3KeyPart(baseName);
                        String destKey = folderPrefix + safeFilename;
                        try {
                            s3Service.copyFile(srcKey, destKey);
                            System.out.println("SUCCESS: Copied " + srcKey + " -> " + destKey);
                        } catch (Exception e) {
                            System.err.println("ERROR: Could not copy " + srcKey + " -> " + destKey + ": " + e.getMessage());
                        }
                    });
                }

                // Remove songs (delete files from S3 playlist folder)
                for (String removeId : toRemove) {
                    songRepository.findById(removeId).ifPresent(song -> {
                        String srcKey = song.getS3Url();
                        String baseName = srcKey.substring(srcKey.lastIndexOf('/') + 1);
                        String safeFilename = sanitizeS3KeyPart(baseName);
                        String destKey = folderPrefix + safeFilename;
                        try {
                            s3Service.deleteFile(destKey);
                            System.out.println("SUCCESS: Deleted " + destKey);
                        } catch (Exception e) {
                            System.err.println("ERROR: Could not delete " + destKey + ": " + e.getMessage());
                        }
                    });
                }
            }
            return playlistRepository.save(existing);
        });
    }

    public List<Playlist> getAllPlaylists() {
        return playlistRepository.findAll();
    }

    public Optional<Playlist> getPlaylistById(String id) {
        return playlistRepository.findById(id);
    }

    public void deletePlaylist(String id) {
        playlistRepository.findById(id).ifPresent(playlist -> {
            if (playlist.getCoverArtUrl() != null && !playlist.getCoverArtUrl().isEmpty())
                s3Service.deleteFile(playlist.getCoverArtUrl());
            playlistRepository.deleteById(id);
        });
    }

}

