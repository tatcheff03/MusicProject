package com.example.musicplatform.repo;

import com.example.musicplatform.Data.Playlist;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlaylistRepository  extends MongoRepository<Playlist, String>{
}
