package com.petcare.backend.model.converter;

import com.petcare.backend.model.enums.SocialProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SocialProviderConverter implements AttributeConverter<SocialProvider, String> {
    @Override
    public String convertToDatabaseColumn(SocialProvider attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SocialProvider convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SocialProvider.fromValue(dbData);
    }
}
