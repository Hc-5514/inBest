package com.jrjr.inbest.board.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.jrjr.inbest.board.dto.BoardDTO;
import com.jrjr.inbest.board.dto.BoardImgDTO;
import com.jrjr.inbest.board.dto.CommentDTO;
import com.jrjr.inbest.global.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Document(collection  = "comment")
@Getter
@NoArgsConstructor
@ToString
@Slf4j
public class CommentEntity extends BaseEntity implements Serializable {
	@Id
	@Field(targetType = FieldType.OBJECT_ID)
	private String id;
	private Long likes;
	private Long userSeq;
	private String context;

	@DBRef
	List<CoCommentEntity> coCommentEntityList;
	private List<UserEntity> likeUserList;

	@Builder
	public CommentEntity(
 		Long likes,
 		Long userSeq,
 		String context){
		this.userSeq = userSeq;
		this.likes = likes;
		this.context = context;
	}

	public CommentDTO toCommentDTO(){
		CommentDTO commentDTO = CommentDTO.builder().
			seq(id).userSeq(userSeq).
			likes(likes).context(context).createdDate(getCreatedDate()).lastModifiedDate(getLastModifiedDate()).
			build();

		List<CommentDTO> cocommentDTOList = new ArrayList<>();
		if(coCommentEntityList != null){
			for(CoCommentEntity coCommentEntity : coCommentEntityList){
				cocommentDTOList.add(coCommentEntity.toCommentDTO());
			}
		}


		commentDTO.setCocommentList(cocommentDTOList);

		return commentDTO;
	}

	public void updateLikeUserList(UserEntity userEntity){
		boolean alreadyLike = false;

		if(likeUserList == null){
			this.likeUserList = new ArrayList<>();
		}
		log.info(likeUserList.toString());

		for(int i=0;i<likeUserList.size();i++){
			//이미 좋아요를 누른 유저인 경우 해당 유저 제거
			if(likeUserList.get(i).getSeq() == userEntity.getSeq()){
				log.info(userEntity.getNickname() +"의 좋아요를 취소합니다.");
				likeUserList.remove(i);
				alreadyLike = true;
			}
		}
		//좋아요를 누르지 않은 유저라면
		if(!alreadyLike){
			log.info(userEntity.getNickname() +"의 좋아요를 추가합니다.");
			likeUserList.add(userEntity);
		}
		log.info("반영 후: "+likeUserList+" ");
		//좋아요 수 반영
		likes = Long.valueOf(likeUserList.size());
	}

	public void setCoCommentEntityList(List<CoCommentEntity> coCommentEntityList){
		this.coCommentEntityList = coCommentEntityList;
	}
}
