package com.example.musicplatform.Controllers;


import com.example.musicplatform.Services.SongService;
import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.RestController;


@RestController
@AllArgsConstructor
public class SongController {
    private final SongService songService;


}

