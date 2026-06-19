package com.petcare.backend.model.converter;

import com.petcare.backend.model.enums.ReactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ReactionTypeConverter implements AttributeConverter<ReactionType, String> {
    @Override
    public String convertToDatabaseColumn(ReactionType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ReactionType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReactionType.fromValue(dbData);
    }
}
