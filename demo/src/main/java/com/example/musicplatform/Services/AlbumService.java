package com.example.musicplatform.Services;

import com.example.musicplatform.Data.Album;
import com.example.musicplatform.Data.Song;
import com.example.musicplatform.repo.AlbumRepository;
import com.example.musicplatform.repo.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlbumService {
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final S3Service s3Service;

    @Autowired
    public AlbumService(AlbumRepository albumRepository, SongRepository songRepository, S3Service s3Service) {
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.s3Service = s3Service;
    }

    private String transliterateAndSanitize(String input) {
        // Copy your S3Service transliterateAndSanitize here or inject S3Service to use it
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(S3Service.CYRILLIC_TO_LATIN.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString().replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    public Album createAlbum(Album album, MultipartFile coverArt) throws IOException {
        String folderPrefix = "albums/" + transliterateAndSanitize(album.getTitle()) + "/";

        if (coverArt != null && !coverArt.isEmpty()) {
            String ext = "";
            String original = coverArt.getOriginalFilename();
            if (original != null && original.lastIndexOf('.') != -1) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String safeName = transliterateAndSanitize("cover" + ext);
            String key = folderPrefix + safeName;
            s3Service.uploadFileToKey(coverArt, key);
            album.setCoverArtUrl(key);
        }

        // Set album name on provided songIds if any
        if (album.getSongIds() != null) {
            for (String songId : album.getSongIds()) {
                songRepository.findById(songId).ifPresent(song -> {
                    song.setAlbum(album.getTitle());
                    songRepository.save(song);
                });
            }
        }

        // Gather all songs with that album title (including just-updated)
        List<Song> albumSongs = songRepository.findByAlbum(album.getTitle());
        List<String> songIds = new ArrayList<>();
        for (Song s : albumSongs) {
            songIds.add(s.getId());
            // Copy to S3 album folder if necessary, as before
            String srcKey = s.getS3Url();
            String baseName = srcKey.substring(srcKey.lastIndexOf('/') + 1);
            String safeBaseName = transliterateAndSanitize(baseName);
            String destKey = folderPrefix + safeBaseName;
            if (!srcKey.equals(destKey)) {
                s3Service.copyFile(srcKey, destKey);
            }
        }
        album.setSongIds(songIds);
        return albumRepository.save(album);
    }


    public Optional<Album> updateAlbum(String id, Album update, MultipartFile coverArt) throws IOException {
        return albumRepository.findById(id).map(existing -> {
            // Handle metadata updates
            if (update.getTitle() != null) existing.setTitle(update.getTitle());
            if (update.getReleaseDate() != null) existing.setReleaseDate(update.getReleaseDate());
            if (update.getArtistId() != null) existing.setArtistId(update.getArtistId());


            String folderPrefix = "albums/" + transliterateAndSanitize(existing.getTitle()) + "/";

            // --- Update cover art if provided ---
            if (coverArt != null && !coverArt.isEmpty()) {
                String oldCover = existing.getCoverArtUrl();
                if (oldCover != null && !oldCover.isEmpty()) s3Service.deleteFile(oldCover);
                String ext = "";
                String original = coverArt.getOriginalFilename();
                if (original != null && original.lastIndexOf('.') != -1) {
                    ext = original.substring(original.lastIndexOf('.'));
                }
                String safeName = transliterateAndSanitize("cover" + ext);
                String key = folderPrefix + safeName;
                try {
                    s3Service.uploadFileToKey(coverArt, key);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                existing.setCoverArtUrl(key);
            }
            // --- Auto-assign all songs whose album field matches (including added/changed title) ---
            List<Song> albumSongs = songRepository.findByAlbum(existing.getTitle()); // always current title!
            List<String> songIds = new ArrayList<>();
            for (Song song : albumSongs) {
                if (!existing.getSongIds().contains(song.getId())) {
                    // Only add if not already present
                    songIds.add(song.getId());
                }
                // Ensure song points to this album title (defensive but safe)
                song.setAlbum(existing.getTitle());
                songRepository.save(song);

                // Copy file to albums folder if not there
                String srcKey = song.getS3Url();
                String baseName = srcKey.substring(srcKey.lastIndexOf('/') + 1);
                String safeBaseName = transliterateAndSanitize(baseName);
                String destKey = folderPrefix + safeBaseName;
                if (!srcKey.equals(destKey)) {
                    s3Service.copyFile(srcKey, destKey);
                }
            }

            // Also merge in any new songIds provided by client (if needed)
            if (update.getSongIds() != null) {
                for (String songId : update.getSongIds()) {
                    if (!songIds.contains(songId)) {
                        songIds.add(songId);
                        songRepository.findById(songId).ifPresent(song -> {
                            song.setAlbum(existing.getTitle());
                            songRepository.save(song);
                            // Copy to album folder if needed
                            String srcKey = song.getS3Url();
                            String baseName = srcKey.substring(srcKey.lastIndexOf('/') + 1);
                            String safeBaseName = transliterateAndSanitize(baseName);
                            String destKey = folderPrefix + safeBaseName;
                            if (!srcKey.equals(destKey)) {
                                s3Service.copyFile(srcKey, destKey);
                            }
                        });
                    }
                }
            }
            existing.setSongIds(songIds);
            return albumRepository.save(existing);
        });
    }


    public void deleteAlbum(String id) {
        albumRepository.findById(id).ifPresent(album -> {
            if (album.getCoverArtUrl() != null && !album.getCoverArtUrl().isEmpty()) {
                s3Service.deleteFile(album.getCoverArtUrl());
            }
            albumRepository.deleteById(id);
        });
    }

    public List<Album> getAllAlbums() {
        return albumRepository.findAll();
    }

    public Optional<Album> getAlbumById(String id) {
        return albumRepository.findById(id);
    }
}
