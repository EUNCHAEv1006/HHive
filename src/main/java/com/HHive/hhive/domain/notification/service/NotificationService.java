package com.HHive.hhive.domain.notification.service;

import com.HHive.hhive.domain.notification.dto.NotificationRequestDTO;
import com.HHive.hhive.domain.notification.dto.NotificationResponseDTO;
import com.HHive.hhive.domain.notification.entity.Notification;
import com.HHive.hhive.domain.notification.repository.NotificationRepository;
import com.HHive.hhive.domain.relationship.hiveuser.entity.HiveUser;
import com.HHive.hhive.domain.relationship.hiveuser.repository.HiveUserRepository;
import com.HHive.hhive.domain.relationship.notificationuser.entity.UserNotification;
import com.HHive.hhive.domain.relationship.notificationuser.repository.UserNotificationRepository;
import com.HHive.hhive.domain.relationship.partyuser.entity.PartyUser;
import com.HHive.hhive.domain.relationship.partyuser.repository.PartyUserRepository;
import com.HHive.hhive.domain.user.entity.User;
import com.HHive.hhive.domain.user.repository.UserRepository;
import com.HHive.hhive.global.common.CommonResponse;
import com.HHive.hhive.global.exception.common.CustomException;
import com.HHive.hhive.global.exception.common.ErrorCode;
import com.HHive.hhive.global.exception.notification.NotificationNotFoundException;
import com.HHive.hhive.global.exception.user.UserNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PartyUserRepository partyUserRepository;
    private final HiveUserRepository hiveUserRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;


    @Transactional
    public CommonResponse sendNotification(NotificationRequestDTO notificationRequestDTO) {
        String type = notificationRequestDTO.getType();
        Notification notification = Notification.builder()
                .message(notificationRequestDTO.getMessage())
                .build();
        notificationRepository.save(notification);
        if (type.equals("party")) {
            List<PartyUser> partyUserList = partyUserRepository.findUsersByPartyId(
                    notificationRequestDTO.getId());
            sendNotificationToUserListParty(partyUserList, notification);
        } else if (type.equals("hive")) {
            List<HiveUser> hiveUserList = hiveUserRepository.findUsersByHiveId(
                    notificationRequestDTO.getId());
            sendNotificationToUserListHive(hiveUserList, notification);
        }

        return new CommonResponse(200, "알림전송 성공", notification);
    }

    public List<NotificationResponseDTO> getNotificationsByUserId(Long userId) {
        List<Notification> notifications = userNotificationRepository
                .findNotificationsByUserIdAndStatus(userId, "unread");

        if (notifications.isEmpty()) {

            throw new NotificationNotFoundException();
        }

        return notifications.stream()
                .map(NotificationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommonResponse deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationNotFoundException::new);
        userNotificationRepository.deleteByNotificationId(notificationId);
        notificationRepository.deleteById(notificationId);

        return new CommonResponse(200, "알림 삭제 완료", null);
    }

    public CommonResponse showgetUnreadNotificationCountForUser(Long userId){
        Long numberOfNotifications = userNotificationRepository
                .countUnreadNotificationsByUserId(userId, "unread");

        return new CommonResponse(200,"d",numberOfNotifications);
    }

    private void sendNotificationToUserListParty(List<PartyUser> partyUserList,
            Notification notification) {
        List<Long> userIdList = partyUserList.stream()
                .map(partyUser -> partyUser.getUser().getId())
                .toList();

        for (Long userId : userIdList) {
            User user = userRepository.findById(userId)
                    .orElseThrow(UserNotFoundException::new);
            UserNotification userNotification = UserNotification.builder()
                    .user(user)
                    .notification(notification)
                    .build();
            userNotificationRepository.save(userNotification);
        }
    }

    private void sendNotificationToUserListHive(List<HiveUser> hiveUserList,
            Notification notification) {
        List<Long> userIdList = hiveUserList.stream()
                .map(hiveUser -> hiveUser.getUser().getId())
                .toList();

        for (Long userId : userIdList) {
            User user = userRepository.findById(userId)
                    .orElseThrow(UserNotFoundException::new);
            UserNotification userNotification = UserNotification.builder()
                    .user(user)
                    .notification(notification)
                    .build();
            userNotificationRepository.save(userNotification);
        }

    }
}
