package com.example.musicplatform.Services;

import com.example.musicplatform.Data.Song;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.musicplatform.repo.SongRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Date;

@Service
public class SongService {

    private final SongRepository songRepository;
    private final S3Service s3Service;

    @Autowired
    public SongService(SongRepository songRepository, S3Service s3Service) {
        this.songRepository = songRepository;
        this.s3Service = s3Service;
    }

//    @PostConstruct
//    public void init() {
//        System.out.println("Song count before insert: " + songRepository.count());
//        Song song = new Song();
//        song.setTitle("Test Title");
//        song.setArtist("Test Artist");
//        song.setGenre("Pop");
//        song.setS3Url("songs/test.mp3");
//        song.setReleaseDate(new Date());
//        song.setDuration(180);
//        song.setAlbum("Test Album");
//        song.setCoverArtUrl("songs/cover.jpg");
//        songRepository.save(song);
//        System.out.println("Inserted test song into MongoDB");
//        System.out.println("Song count after insert: " + songRepository.count());
//    }

    public Song createSong(Song song) {
        return songRepository.save(song);
    }

    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    public Optional<Song> updateSongMetadata(String id, Song updatedSong) {
        return songRepository.findById(id).map(existingSong -> {
            if (updatedSong.getTitle() != null) existingSong.setTitle(updatedSong.getTitle());
            if (updatedSong.getArtist() != null) existingSong.setArtist(updatedSong.getArtist());
            if (updatedSong.getGenre() != null) existingSong.setGenre(updatedSong.getGenre());
            if (updatedSong.getAlbum() != null) existingSong.setAlbum(updatedSong.getAlbum());
            if (updatedSong.getDuration() != null) existingSong.setDuration(updatedSong.getDuration());
            if (updatedSong.getReleaseDate() != null) existingSong.setReleaseDate(updatedSong.getReleaseDate());
            // Do NOT touch s3Url or coverArtUrl here
            return songRepository.save(existingSong);
        });
    }

    public Optional<Song> updateSongWithFiles(String id, Song updatedSong, MultipartFile file, MultipartFile coverArt) throws IOException {
        return songRepository.findById(id).map(existingSong -> {

            // Update metadata fields only if non-null
            if (updatedSong.getTitle() != null) existingSong.setTitle(updatedSong.getTitle());
            if (updatedSong.getArtist() != null) existingSong.setArtist(updatedSong.getArtist());
            if (updatedSong.getGenre() != null) existingSong.setGenre(updatedSong.getGenre());
            if (updatedSong.getAlbum() != null) existingSong.setAlbum(updatedSong.getAlbum());
            if (updatedSong.getDuration() != null) existingSong.setDuration(updatedSong.getDuration());
            if (updatedSong.getReleaseDate() != null) existingSong.setReleaseDate(updatedSong.getReleaseDate());

            // If new audio file is provided
            if (file != null && !file.isEmpty()) {
                // Delete old audio file from S3 if any
                String oldS3Key = existingSong.getS3Url();
                if (oldS3Key != null && !oldS3Key.isEmpty()) {
                    try {
                        System.out.println("Deleting old audio file from S3: " + oldS3Key);
                        System.out.println("Uploading new S3 audio for song id " + existingSong.getId());
                        s3Service.deleteFile(oldS3Key);
                    } catch (Exception e) {
                        // Log the error but don't stop update
                        System.err.println("Failed to delete old audio from S3: " + e.getMessage());
                    }
                }
                // Upload new audio file
                String newS3Key = null;
                try {
                    newS3Key = s3Service.uploadFile(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                existingSong.setS3Url(newS3Key);
            }

            // If new cover art is provided
            if (coverArt != null && !coverArt.isEmpty()) {
                // Delete old cover art from S3 if any
                String oldCoverKey = existingSong.getCoverArtUrl();
                if (oldCoverKey != null && !oldCoverKey.isEmpty()) {
                    try {
                        s3Service.deleteFile(oldCoverKey);
                    } catch (Exception e) {
                        System.err.println("Failed to delete old cover art from S3: " + e.getMessage());
                    }
                }
                // Upload new cover art file
                String newCoverKey = null;
                try {
                    newCoverKey = s3Service.uploadCoverArt(coverArt);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                existingSong.setCoverArtUrl(newCoverKey);
            }

            // Save updated entity
            return songRepository.save(existingSong);
        });
    }



    public Optional<Song> getSongById(String id) {
        return songRepository.findById(id);
    }

    public void deleteSong(String id) {
        songRepository.deleteById(id);

    }
}
