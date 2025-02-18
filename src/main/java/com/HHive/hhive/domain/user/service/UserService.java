package com.HHive.hhive.domain.user.service;

import com.HHive.hhive.domain.user.dto.*;
import com.HHive.hhive.domain.user.entity.User;
import com.HHive.hhive.domain.user.repository.UserRepository;
import com.HHive.hhive.global.exception.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(UserSignupRequestDTO requestDTO) {
        String username = requestDTO.getUsername();
        String password = requestDTO.getPassword();
        String checkPassword = requestDTO.getCheckPassword();
        String email = requestDTO.getEmail();
        String description = requestDTO.getDescription();

        // 비밀번호 != 비밀번호 확인
        if (!Objects.equals(password, checkPassword)) {
            throw new PasswordConfirmationException();
        }

        String encodePassword = passwordEncoder.encode(password);

        // 유저네임 중복확인
        if (userRepository.findByUsername(username).isPresent()) {
            throw new AlreadyExistUsernameException();
        }

        // 이메일 중복확인
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AlreadyExistEmailException();
        }

        User user = new User(username, encodePassword, email, description);
        userRepository.save(user);
    }

    public void login(UserLoginRequestDTO requestDTO) {
        String username = requestDTO.getUsername();
        String password = requestDTO.getPassword();

        // 저장된 회원이 없을 때
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserNotFoundException();
        }
    }

    public UserInfoResponseDTO getProfile(Long user_id) {
        User user = getUser(user_id);
        return new UserInfoResponseDTO(user);
    }

    @Transactional
    public void updateProfile(Long user_id, UpdateUserProfileRequestDTO requestDTO, User loginUser) {

        User user = getUser(user_id);

        // 로그인한 유저 == 수정한 프로필의 유저 확인
        if (!loginUser.getUsername().equals(user.getUsername())) {
            throw new AuthenticationMismatchException();
        }

        user.updateProfile(requestDTO);
    }

    @Transactional
    public void updatePassword(Long user_id, UpdateUserPasswordRequestDTO requestDTO, User loginUser) {
        String password = requestDTO.getPassword();
        String updatePassword = requestDTO.getUpdatePassword();
        String checkUpdatePassword = requestDTO.getCheckUpdatePassword();

        User user = getUser(user_id);

        if (!loginUser.getUsername().equals(user.getUsername())) {
            throw new AuthenticationMismatchException();
        }

        if (!updatePassword.equals(checkUpdatePassword)) {
            throw new PasswordConfirmationException();
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordMismatchException();
        } else {
            updatePassword = passwordEncoder.encode(updatePassword);
            user.setPassword(updatePassword);
        }

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long user_id, User loginUser) {
        User userToDelete = getUser(user_id);

        // 로그인 한 유저 == 삭제할 유저 확인
        if (!loginUser.getUsername().equals(userToDelete.getUsername())) {
            throw new AuthenticationMismatchException();
        }

        // 삭제 -> Soft delete로 구현
        userToDelete.updateDeletedAt();

        userRepository.save(userToDelete);
    }

    @Scheduled(fixedRate = 60 * 1000) // 1분마다 실행
    public void processPendingDeletions() {
        List<User> userToDelete = userRepository.findByIsDeletedAndDeletedAtBefore(true, LocalDateTime.now().minusMinutes(1));

        userRepository.deleteAll(userToDelete);
    }

    public User getUser(Long user_id) {
        return userRepository.findById(user_id).orElseThrow(UserNotFoundException::new);
    }
}
