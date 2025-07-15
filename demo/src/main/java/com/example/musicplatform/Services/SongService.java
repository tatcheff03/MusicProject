package com.example.musicplatform.Services;

import com.example.musicplatform.Data.Song;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.musicplatform.repo.SongRepository;

import java.util.Date;

@Service
public class SongService {

    private final SongRepository songRepository;

    @Autowired
    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
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
}

