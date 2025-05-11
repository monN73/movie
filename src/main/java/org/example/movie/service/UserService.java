package org.example.movie.service;

import org.example.movie.dto.UserDto;
import org.example.movie.entity.User;
import org.example.movie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String BASE_PATH = "/app/uploads/"; // 本地保存路径
    private static final String URL_PREFIX = "/images/";
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        User user = userRepository.findUserByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 返回 Spring Security 需要的 UserDetails 对象
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getName())
                .password(user.getPassword())
                .roles(user.getRole()) // 例如 "ROLE_USER" 或 "ROLE_ADMIN"
                .build();
    }


    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    UserDto userDto = new UserDto();
                    userDto.setUsername(user.getName());
                    userDto.setAvatarUrl(user.getAvatarUrl());
                    userDto.setPosterUrl(user.getPosterUrl());
                    return userDto;
                });
    }

    public String getUserName(Long id) {
        return userRepository.findById(id).get().getName();
    }


    public Optional<User> registerUser(String username, String password) {
        Optional<User> existingUser = userRepository.findUserByName(username);
        if(existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setName(username);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRole("USER");  // 默认赋予普通用户角色
            User savedUser = userRepository.save(newUser);
            return Optional.of(savedUser);
        } else {
            return Optional.empty();
        }
    }

    public boolean deleteUser(String username, String password) {
        Optional<User> existingUser = userRepository.findUserByName(username);
        if(existingUser.isPresent()) {
            User user = existingUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                userRepository.delete(user);
                return true;
            }
        }
        return false;
    }

    public boolean uploadAvatar(MultipartFile file,Long userId) throws IOException {

        Optional<User> userOpt = userRepository.findById(userId);
        if(!userOpt.isPresent()) return false;
        User user = userOpt.get();

        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            deleteOldFile(oldAvatarUrl);
        }

        // 获取文件后缀名
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());

        // 生成 UUID 文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "." + ext;

        // 使用哈希前两位创建子目录
        String dir1 = uuid.substring(0, 2);
        String dir2 = uuid.substring(2, 4);
        File dir = new File(BASE_PATH + dir1 + "/" + dir2);
        if (!dir.exists()) dir.mkdirs();

        // 保存文件
        File dest = new File(dir, fileName);
        file.transferTo(dest);

        // 拼接访问 URL
        String avatarUrl = URL_PREFIX + dir1 + "/" + dir2 + "/" + fileName;


        // 保存路径到数据库
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return true;
    }

    public boolean uploadPoster(MultipartFile file,Long userId) throws IOException {

        Optional<User> userOpt = userRepository.findById(userId);
        if(!userOpt.isPresent()) return false;
        User user = userOpt.get();

        String oldPosterUrl = user.getPosterUrl();
        if (oldPosterUrl != null && !oldPosterUrl.isEmpty()) {
            deleteOldFile(oldPosterUrl);
        }

        // 获取文件后缀名
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());

        // 生成 UUID 文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "." + ext;

        // 使用哈希前两位创建子目录
        String dir1 = uuid.substring(0, 2);
        String dir2 = uuid.substring(2, 4);
        File dir = new File(BASE_PATH + dir1 + "/" + dir2);
        if (!dir.exists()) dir.mkdirs();

        // 保存文件
        File dest = new File(dir, fileName);
        file.transferTo(dest);

        // 拼接访问 URL
        String posterUrl = URL_PREFIX + dir1 + "/" + dir2 + "/" + fileName;

        // 保存路径到数据库
        user.setPosterUrl(posterUrl);
        userRepository.save(user);

        return true;
    }

    private void deleteOldFile(String oldUrl) {
        try {
            // 从URL中提取相对路径
            String relativePath = oldUrl.replace(URL_PREFIX, "");
            Path oldFilePath = Paths.get(BASE_PATH, relativePath);

            // 删除文件
            if (Files.exists(oldFilePath)) {
                Files.delete(oldFilePath);

                // 尝试删除空目录（可选）
                Path parentDir = oldFilePath.getParent();
                if (Files.list(parentDir).count() == 0) {
                    Files.delete(parentDir);
                    Path grandParentDir = parentDir.getParent();
                    if (Files.list(grandParentDir).count() == 0) {
                        Files.delete(grandParentDir);
                    }
                }
            }
        } catch (IOException e) {
            // 删除失败不影响主流程，但应该记录日志
            log.error("删除旧头像文件失败: {}", oldUrl, e);
        }
    }


}
