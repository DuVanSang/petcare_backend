package com.petcare.backend.dto.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionSummaryResponse {
    private long total;
    private long like;
    private long love;
    private long haha;
    private long wow;
    private long sad;
    private long angry;
    private long care;
    private String currentUserReaction;

    public static ReactionSummaryResponse empty() {
        return ReactionSummaryResponse.builder()
                .total(0L)
                .like(0L)
                .love(0L)
                .haha(0L)
                .wow(0L)
                .sad(0L)
                .angry(0L)
                .care(0L)
                .build();
    }
}
