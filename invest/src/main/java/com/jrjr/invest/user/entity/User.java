package com.jrjr.invest.user.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import com.jrjr.invest.global.entity.BaseEntity;
import com.jrjr.invest.simulation.entity.SimulationUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long seq;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String nickname;

	private String birthyear;

	private String birthday;

	@ColumnDefault("0")
	private Integer gender;

	private String profileImgSearchName;

	private String profileImgOriginalName;

	private LocalDateTime deletedDate;

	@OneToMany(mappedBy = "user")
	private List<SimulationUser> simulationUserList;

	@Builder
	public User(String email, String name, String nickname, String birthyear, String birthday, Integer gender,
		String profileImgSearchName, String profileImgOriginalName) {
		this.email = email;
		this.name = name;
		this.nickname = nickname;
		this.birthyear = birthyear;
		this.birthday = birthday;
		this.gender = gender;
		this.profileImgSearchName = profileImgSearchName;
		this.profileImgOriginalName = profileImgOriginalName;
	}

	@Override
	public String toString() {
		return "User{" +
			"email='" + email + '\'' +
			", name='" + name + '\'' +
			", nickname='" + nickname + '\'' +
			", birthyear='" + birthyear + '\'' +
			", birthday='" + birthday + '\'' +
			", gender=" + gender +
			", profileImgSearchName='" + profileImgSearchName + '\'' +
			", profileImgOriginalName='" + profileImgOriginalName + '\'' +
			", deletedDate=" + deletedDate +
			'}';
	}
}
