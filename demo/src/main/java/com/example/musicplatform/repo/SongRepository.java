package com.example.musicplatform.repo;

import com.example.musicplatform.Data.Song;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SongRepository extends MongoRepository<Song, String>{
    List<Song> findByAlbum(String album);
}
