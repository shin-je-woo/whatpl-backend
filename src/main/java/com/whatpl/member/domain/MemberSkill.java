package com.whatpl.member.domain;

import com.whatpl.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "member_skill")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSkill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Skill skill;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public MemberSkill(Skill skill, Member member) {
        this.skill = skill;
        this.member = member;
    }
}
