package org.example.movie.controller;


import org.example.movie.dto.UserDto;
import org.example.movie.entity.User;
import org.example.movie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/users")  // 统一的 API 路径前缀
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        Optional<User> newuser = userService.registerUser(user.getName(), user.getPassword());
        return ResponseEntity.ok(newuser);
    }


    /**
     * 查询所有用户
     */
    @GetMapping("/allusers")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUser();
        return ResponseEntity.ok(users);
    }

    /**
     * 根据 ID 查询用户
     */
    @GetMapping("/get")
    public ResponseEntity<?> getUserById(@RequestParam Long userId) {
        Optional<UserDto> user = (Optional<UserDto>) userService.getUserById(userId);

        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestBody User user) {
        if(userService.deleteUser(user.getName(), user.getPassword())){
            return ResponseEntity.ok("User deleted successfully!");
        }else{
            return ResponseEntity.status(404).body("User don't exist!");
        }
    }

    @PostMapping("/upload/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,@RequestParam Long userId) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        List<String> allowed = Arrays.asList("jpg", "png", "jpeg");
        if (!allowed.contains(ext.toLowerCase())) {
            return ResponseEntity.badRequest().body("文件不支持");
        }

        if(userService.uploadAvatar(file,userId)){
            return ResponseEntity.ok("Avatar uploaded successfully!");
        }else{
            return ResponseEntity.status(404).body("Avatar don't exist!");
        }

    }

    @PostMapping("/upload/poster")
    public ResponseEntity<?> uploadPoster(@RequestParam("file") MultipartFile file,@RequestParam Long userId) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        List<String> allowed = Arrays.asList("jpg", "png", "jpeg");
        if (!allowed.contains(ext.toLowerCase())) {
            return ResponseEntity.badRequest().body("文件不支持");
        }

        if(userService.uploadPoster(file,userId)){
            return ResponseEntity.ok("Poster uploaded successfully!");
        }else{
            return ResponseEntity.status(404).body("Poster don't exist!");
        }

    }




}

