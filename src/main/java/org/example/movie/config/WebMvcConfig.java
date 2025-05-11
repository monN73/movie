package org.example.movie.config;

import org.example.movie.security.UserPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private UserPermissionInterceptor userPermissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userPermissionInterceptor)
                .addPathPatterns("/movieList/add",
                                 "/movieList/delete",
                        "/comments/add",
                        "/comments/delete",
                        "/users/upload/avatar",
                        "/users/upload/poster");  // 你需要保护的路径
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/app/uploads/"); // 指向实际图片路径
    }

}
