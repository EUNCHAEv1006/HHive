package com.HHive.hhive.domain.party.entity;

import com.HHive.hhive.domain.hive.entity.Hive;
import com.HHive.hhive.domain.party.request.PartyRequestDTO;
import com.HHive.hhive.domain.user.entity.User;
import com.HHive.hhive.global.auditing.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Party extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @ManyToOne
    @JoinColumn(name = "hive_id", nullable = false)
    private Hive hive;

    @Column
    private String title;

    @Column
    private String username;

    @Column(length = 1500)
    private String content;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    public Party(String title, String username, String content) {
        this.title = title;
        this.username = username;
        this.content = content;
    }

    public Party(Hive hive, PartyRequestDTO dto, User user) {
        this.hive = hive;
        this.title = dto.getTitle();
        this.username = user.getUsername();
        this.content = dto.getContent();
        this.hostId = user.getId();
    }

    // 연관관계 메서드
    public void setUser(User user) {
        this.user = user;
    }

    // 서비스 메서드
    public void setTitle(String partyTitle) {
        this.title = partyTitle;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setContent(String partyContent) {
        this.content = partyContent;
    }
}
