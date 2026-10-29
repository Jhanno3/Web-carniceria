package com.carniceria.backend.controller;

import com.carniceria.backend.dto.request.UpdateProfileRequestDto;
import com.carniceria.backend.dto.response.ProfileResponseDto;
import com.carniceria.backend.mapper.ProfileMapper;
import com.carniceria.backend.security.CurrentUser;
import com.carniceria.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sin lógica de negocio: todo se delega a ProfileService. Esta clase solo mapea HTTP <-> dto.
 */
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileMapper mapper;

    public ProfileController(ProfileService profileService, ProfileMapper mapper) {
        this.profileService = profileService;
        this.mapper = mapper;
    }

    @GetMapping("/profile/me")
    public ProfileResponseDto getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponseDto(profileService.getMyProfile(CurrentUser.userId(jwt)));
    }

    @PutMapping("/profile/me")
    public ProfileResponseDto updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody UpdateProfileRequestDto dto) {
        return mapper.toResponseDto(profileService.updateMyProfile(CurrentUser.userId(jwt), dto));
    }

    @GetMapping("/profiles/{id}")
    public ProfileResponseDto getProfileById(@AuthenticationPrincipal Jwt jwt,
                                              Authentication authentication,
                                              @PathVariable UUID id) {
        return mapper.toResponseDto(profileService.getProfileById(CurrentUser.userId(jwt), CurrentUser.role(authentication), id));
    }

    @GetMapping("/profiles")
    public List<ProfileResponseDto> listAllProfiles(Authentication authentication) {
        return profileService.listAllProfiles(CurrentUser.role(authentication)).stream().map(mapper::toResponseDto).toList();
    }
}
