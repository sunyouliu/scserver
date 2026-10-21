package com.example.documentstore.auth;

import com.example.documentstore.auth.dto.UpdateUserProfileRequest;
import com.example.documentstore.auth.dto.UserProfileResponse;
import com.example.documentstore.auth.dto.WechatLoginRequest;
import com.example.documentstore.auth.dto.WechatLoginResponse;
import com.example.documentstore.common.ApiException;
import com.example.documentstore.config.AppProperties;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.repository.UserAccountRepository;
import com.example.documentstore.security.JwtService;
import com.example.documentstore.security.SessionKeyCryptoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppProperties properties;
    private final WechatApiClient wechatApiClient;
    private final UserAccountRepository userRepository;
    private final SessionKeyCryptoService sessionKeyCryptoService;
    private final JwtService jwtService;

    public AuthService(
            AppProperties properties,
            WechatApiClient wechatApiClient,
            UserAccountRepository userRepository,
            SessionKeyCryptoService sessionKeyCryptoService,
            JwtService jwtService
    ) {
        this.properties = properties;
        this.wechatApiClient = wechatApiClient;
        this.userRepository = userRepository;
        this.sessionKeyCryptoService = sessionKeyCryptoService;
        this.jwtService = jwtService;
    }

    @Transactional
    public WechatLoginResponse login(WechatLoginRequest request) {
        WechatApiClient.WechatSession session = wechatApiClient.exchangeCode(request.code());
        UserAccount user = userRepository.findByOpenId(session.openId()).orElseGet(UserAccount::new);
        user.setOpenId(session.openId());
        user.setUnionId(session.unionId());
        user.setEncryptedSessionKey(sessionKeyCryptoService.encrypt(session.sessionKey()));
        updateProfileFields(user, request.nickname(), request.avatarUrl());
        userRepository.save(user);

        return new WechatLoginResponse(
                jwtService.createToken(user.getId()),
                properties.security().jwtExpirationSeconds(),
                toProfile(user)
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return toProfile(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserAccount user = findUser(userId);
        updateProfileFields(user, request.nickname(), request.avatarUrl());
        return toProfile(user);
    }

    private UserAccount findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "登录用户不存在"));
    }

    private void updateProfileFields(UserAccount user, String nickname, String avatarUrl) {
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl.trim());
        }
    }

    private UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(user.getId(), user.getNickname(), user.getAvatarUrl());
    }
}
