package org.example.movie.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.movie.entity.User;
import org.example.movie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class UserPermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Optional<User> currentUserOpt = userRepository.findUserByName(username);
        if (currentUserOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未找到当前用户信息");
            return false;
        }

        User currentUser = currentUserOpt.get();

        // ✅ 从查询参数中获取 userId
        String userIdParam = request.getParameter("userId");
        if (userIdParam == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少用户 ID 参数");
            return false;
        }

        Long pathUserId;
        try {
            pathUserId = Long.parseLong(userIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "用户 ID 参数格式错误");
            return false;
        }

        // 校验是否为当前用户或管理员
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isOwner = pathUserId.equals(currentUser.getUid());

        if (!isOwner && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "没有权限操作他人的数据");
            return false;
        }

        return true;
    }

}
