package com.petcare.backend.model.converter;

import com.petcare.backend.model.enums.PostStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PostStatusConverter implements AttributeConverter<PostStatus, String> {
    @Override
    public String convertToDatabaseColumn(PostStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PostStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PostStatus.fromValue(dbData);
    }
}
