package com.example.musicplatform.repo;

import com.example.musicplatform.Data.Album;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AlbumRepository extends MongoRepository<Album, String>{
}
