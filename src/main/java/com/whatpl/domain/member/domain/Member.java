package com.whatpl.domain.member.domain;

import com.whatpl.domain.attachment.domain.Attachment;
import com.whatpl.global.common.model.BaseTimeEntity;
import com.whatpl.global.common.model.*;
import com.whatpl.global.exception.BizException;
import com.whatpl.global.exception.ErrorCode;
import com.whatpl.domain.member.model.MemberStatus;
import com.whatpl.domain.member.model.SocialType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String socialId;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    private Job job;

    @Enumerated(EnumType.STRING)
    private Career career;

    @Enumerated(EnumType.STRING)
    private WorkTime workTime;

    private Boolean profileOpen;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "picture_id")
    private Attachment picture;

    @OrderBy("id")
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberSkill> memberSkills = new LinkedHashSet<>();

    @OrderBy("id")
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberSubject> memberSubjects = new LinkedHashSet<>();

    @OrderBy("id")
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberReference> memberReferences = new LinkedHashSet<>();

    @OrderBy("id")
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberPortfolio> memberPortfolios = new LinkedHashSet<>();

    @Builder
    public Member(SocialType socialType, String socialId, String nickname, MemberStatus status,
                  Job job, Career career, WorkTime workTime, Boolean profileOpen) {
        this.socialType = socialType;
        this.socialId = socialId;
        this.nickname = nickname;
        this.status = status;
        this.job = job;
        this.career = career;
        this.workTime = workTime;
        this.profileOpen = profileOpen;
    }

    //==연관관계 메서드==//
    public void addMemberSkill(MemberSkill memberSkill) {
        if (memberSkill == null) {
            return;
        }
        if (memberSkills.size() >= 10) {
            throw new BizException(ErrorCode.MAX_SKILL_SIZE_EXCEED);
        }
        this.memberSkills.add(memberSkill);
        memberSkill.addRelation(this);
    }

    public void addMemberSubject(MemberSubject memberSubject) {
        if (memberSubject == null) {
            return;
        }
        if (memberReferences.size() >= 6) {
            throw new BizException(ErrorCode.MAX_SUBJECT_SIZE_EXCEED);
        }
        this.memberSubjects.add(memberSubject);
        memberSubject.addRelation(this);
    }

    public void addMemberReference(MemberReference memberReference) {
        if (memberReference == null) {
            return;
        }
        if (memberReferences.size() >= 5) {
            throw new BizException(ErrorCode.MAX_REFERENCE_SIZE_EXCEED);
        }
        this.memberReferences.add(memberReference);
        memberReference.addRelation(this);
    }

    public void addMemberPortfolio(MemberPortfolio memberPortfolio) {
        if (memberPortfolio == null) {
            return;
        }
        if (memberPortfolios.size() >= 5) {
            throw new BizException(ErrorCode.MAX_PORTFOLIO_SIZE_EXCEED);
        }
        this.memberPortfolios.add(memberPortfolio);
        memberPortfolio.addRelation(this);
    }

    public void modifyPicture(Attachment picture) {
        this.picture = picture;
    }

    //==비즈니스 로직==//

    /**
     * 필수 정보 입력
     */
    public void modifyRequiredInfo(String nickname, Job job, Career career, boolean profileOpen) {
        this.nickname = nickname;
        this.job = job;
        this.career = career;
        this.profileOpen = profileOpen;
    }

    /**
     * 프로필 작성 여부
     * 필수 정보만 입력하면 작성한 것으로 판단
     */
    public boolean hasProfile() {
        return (!CollectionUtils.isEmpty(memberSkills) &&
                getJob() != null &&
                getCareer() != null &&
                getNickname() != null &&
                getProfileOpen() != null);
    }

    /**
     * memberSkills를 추가/삭제한다.
     */
    public void modifyMemberSkills(Set<Skill> skills) {
        if (CollectionUtils.isEmpty(skills)) {
            throw new IllegalStateException("skills cannot be empty");
        }
        memberSkills.clear();
        skills.stream()
                .map(MemberSkill::new)
                .forEach(this::addMemberSkill);
    }

    /**
     * memberSubjects를 추가/삭제한다.
     */
    public void modifyMemberSubject(Set<Subject> subjects) {
        if (CollectionUtils.isEmpty(subjects)) {
            throw new IllegalStateException("subjects cannot be empty");
        }
        memberSubjects.clear();
        subjects.stream()
                .map(MemberSubject::new)
                .forEach(this::addMemberSubject);
    }

    /**
     * memberReferences를 추가/삭제한다.
     */
    public void modifyMemberReference(Set<String> references) {
        memberReferences.clear();
        Optional.ofNullable(references)
                .orElseGet(Collections::emptySet).stream()
                .map(MemberReference::new)
                .forEach(this::addMemberReference);
    }

    public void modifyWorkTime(@NonNull WorkTime workTime) {
        this.workTime = workTime;
    }

    public MemberEditor.MemberEditorBuilder toEditor() {
        return MemberEditor.builder()
                .nickname(nickname)
                .job(job)
                .career(career)
                .profileOpen(profileOpen)
                .workTime(workTime);
    }

    public void updateProfile(
            MemberEditor editor,
            Set<Subject> subjects,
            Set<String> references,
            Set<Skill> skills
    ) {
        this.nickname = editor.getNickname();
        this.job = editor.getJob();
        this.career = editor.getCareer();
        this.workTime = editor.getWorkTime();
        this.profileOpen = editor.isProfileOpen();
        modifyMemberSubject(subjects);
        modifyMemberReference(references);
        modifyMemberSkills(skills);
    }

    public List<MemberPortfolio> deletePortfolios(List<Long> deletePortfolioIds) {
        if (CollectionUtils.isEmpty(deletePortfolioIds) || CollectionUtils.isEmpty(getMemberPortfolios())) {
            return Collections.emptyList();
        }
        Set<MemberPortfolio> deleteList = getMemberPortfolios().stream()
                .filter(memberPortfolio -> deletePortfolioIds.stream()
                        .anyMatch(deleteId -> deleteId.equals(memberPortfolio.getId())))
                .collect(Collectors.toSet());
        getMemberPortfolios().removeAll(deleteList);
        return deleteList.stream().toList();
    }
}