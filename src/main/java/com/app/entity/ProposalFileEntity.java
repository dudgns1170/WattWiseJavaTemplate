package com.app.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 제안서 파일 엔티티
 * 
 * proposal_file 테이블과 매핑되는 도메인 객체입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposalFileEntity {
    private String pr_no;
    private String pr_user_id;
    private String pr_co_name;
    private String pr_site_address;
    private String pr_capacity;
    private String pr_file_name;
    private String pr_file_object_key;
    private String pr_file_storage_url;
    private String pr_file_created_by_name;
    private String pr_file_updated_by_name;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
