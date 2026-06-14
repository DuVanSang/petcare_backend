package com.petcare.backend.model.converter;

import com.petcare.backend.model.enums.PostPrivacy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PostPrivacyConverter implements AttributeConverter<PostPrivacy, String> {
    @Override
    public String convertToDatabaseColumn(PostPrivacy attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PostPrivacy convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PostPrivacy.fromValue(dbData);
    }
}
