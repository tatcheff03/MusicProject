package com.example.musicplatform.repo;

import com.example.musicplatform.Data.Song;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SongRepository extends MongoRepository<Song, String>{
}
